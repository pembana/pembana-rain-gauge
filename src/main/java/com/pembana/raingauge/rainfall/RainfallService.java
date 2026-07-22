/*
 * Copyright 2026 Gunnar Hillert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pembana.raingauge.rainfall;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.ObservationService;
import com.pembana.raingauge.station.RainfallCapability;
import com.pembana.raingauge.station.RainfallCapabilityService;
import com.pembana.raingauge.station.Station;

import org.springframework.stereotype.Service;

/**
 * Provides rainfall operations.
 * @author Gunnar Hillert
 */
@Service
public class RainfallService {

	private final ObservationService observationService;

	private final RainfallAccumulator rainfallAccumulator;

	private final ObservationCadenceDetector cadenceDetector;

	private final RainfallCapabilityService capabilityService;

	private final RainfallProperties properties;

	private final Clock clock;

	/**
	 * Creates a new {@code RainfallService}.
	 * @param observationService the observation service
	 * @param rainfallAccumulator the rainfall accumulator
	 * @param cadenceDetector the cadence detector
	 * @param capabilityService the capability service
	 * @param properties the rainfall application properties
	 * @param clock the clock used to obtain the current time
	 */
	public RainfallService(ObservationService observationService,
			RainfallAccumulator rainfallAccumulator, ObservationCadenceDetector cadenceDetector,
			RainfallCapabilityService capabilityService, RainfallProperties properties, Clock clock) {
		this.observationService = observationService;
		this.rainfallAccumulator = rainfallAccumulator;
		this.cadenceDetector = cadenceDetector;
		this.capabilityService = capabilityService;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Calculates rainfall for the requested interval.
	 * @param station the station to process
	 * @param window the requested rainfall window
	 * @param unit the requested rainfall unit
	 * @return the calculated rainfall result
	 */
	public RainfallResult calculate(Station station, RainfallWindow window, RainfallUnit unit) {
		RainfallWindow.TimeRange range = window.resolve(this.clock.instant());
		return calculate(station, range.from(), range.to(), unit);
	}

	/**
	 * Calculates all dashboard rainfall windows.
	 * @param station the station to process
	 * @param windows the windows
	 * @param unit the requested rainfall unit
	 * @return the calculated rainfall result
	 */
	public Map<RainfallWindow, RainfallResult> calculateWindows(Station station,
			Set<RainfallWindow> windows, RainfallUnit unit) {
		if (windows.isEmpty()) {
			return Map.of();
		}
		Instant now = this.clock.instant();
		Map<RainfallWindow, RainfallWindow.TimeRange> ranges = new EnumMap<>(RainfallWindow.class);
		Instant earliest = now;
		Instant latest = Instant.MIN;
		for (RainfallWindow window : windows) {
			RainfallWindow.TimeRange range = window.resolve(now);
			validateRange(range.from(), range.to());
			ranges.put(window, range);
			if (range.from().isBefore(earliest)) {
				earliest = range.from();
			}
			if (range.to().isAfter(latest)) {
				latest = range.to();
			}
		}
		RainfallCapabilityService.CapabilityDiscovery capability =
				this.capabilityService.discover(station);
		station.updateCapability(capability.capability(), capability.precipitationKey());
		if (capability.capability() != RainfallCapability.SUPPORTED_ACCUMULATOR
				|| capability.precipitationKey() == null) {
			throw new UnsupportedRainfallStationException(station.getStationId());
		}
		ObservationBatch observations = this.observationService.observations(station.getStationId(),
				station.getNetwork(), capability.precipitationKey(),
				earliest.minus(this.properties.getQuery().getBaselineLookback()), latest);
		Duration cadence = this.cadenceDetector.detect(observations.observations());
		Map<RainfallWindow, RainfallResult> results = new EnumMap<>(RainfallWindow.class);
		for (Map.Entry<RainfallWindow, RainfallWindow.TimeRange> entry : ranges.entrySet()) {
			RainfallWindow.TimeRange range = entry.getValue();
			results.put(entry.getKey(), this.rainfallAccumulator.calculate(observations.observations(),
					range.from(), range.to(), cadence, observations, unit));
		}
		return Map.copyOf(results);
	}

	/**
	 * Calculates rainfall for the requested interval.
	 * @param station the station to process
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param unit the requested rainfall unit
	 * @return the calculated rainfall result
	 */
	public RainfallResult calculate(Station station, Instant from, Instant to, RainfallUnit unit) {
		validateRange(from, to);
		RainfallCapabilityService.CapabilityDiscovery capability =
				this.capabilityService.discover(station);
		station.updateCapability(capability.capability(), capability.precipitationKey());
		if (capability.capability() != RainfallCapability.SUPPORTED_ACCUMULATOR
				|| capability.precipitationKey() == null) {
			throw new UnsupportedRainfallStationException(station.getStationId());
		}
		Instant queryFrom = from.minus(this.properties.getQuery().getBaselineLookback());
		ObservationBatch observations = this.observationService.observations(station.getStationId(),
				station.getNetwork(), capability.precipitationKey(), queryFrom, to);
		Duration cadence = this.cadenceDetector.detect(observations.observations());
		return this.rainfallAccumulator.calculate(observations.observations(), from, to, cadence,
				observations, unit);
	}

	/**
	 * Validates range.
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 */
	private void validateRange(Instant from, Instant to) {
		if (!from.isBefore(to)) {
			throw new IllegalArgumentException("The rainfall range start must be before its end");
		}
		Duration duration = Duration.between(from, to);
		if (duration.compareTo(this.properties.getQuery().getMaximumRange()) > 0) {
			throw new IllegalArgumentException("The rainfall range exceeds the configured maximum");
		}
		if (to.isAfter(this.clock.instant().plus(Duration.ofMinutes(5)))) {
			throw new IllegalArgumentException("The rainfall range cannot end in the future");
		}
	}

}
