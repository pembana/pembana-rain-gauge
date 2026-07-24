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

import org.springframework.stereotype.Service;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.ObservationService;
import com.pembana.raingauge.observation.shef.ShefPrecipitationCode;
import com.pembana.raingauge.rainfall.cumulative.CumulativeRainfallCalculator;
import com.pembana.raingauge.rainfall.interval.IntervalRainfallCalculator;
import com.pembana.raingauge.station.RainfallCapability;
import com.pembana.raingauge.station.RainfallCapabilityService;
import com.pembana.raingauge.station.Station;

/**
 * Provides rainfall operations.
 * @author Gunnar Hillert
 */
@Service
public class RainfallService {

	private final ObservationService observationService;

	private final CumulativeRainfallCalculator cumulativeCalculator;

	private final IntervalRainfallCalculator intervalCalculator;

	private final ObservationCadenceDetector cadenceDetector;

	private final RainfallCapabilityService capabilityService;

	private final RainfallProperties properties;

	private final Clock clock;

	/**
	 * Creates a new {@code RainfallService}.
	 * @param observationService the observation service
	 * @param cumulativeCalculator the cumulative rainfall calculator
	 * @param intervalCalculator the interval rainfall calculator
	 * @param cadenceDetector the cadence detector
	 * @param capabilityService the capability service
	 * @param properties the rainfall application properties
	 * @param clock the clock used to obtain the current time
	 */
	public RainfallService(ObservationService observationService,
			CumulativeRainfallCalculator cumulativeCalculator,
			IntervalRainfallCalculator intervalCalculator, ObservationCadenceDetector cadenceDetector,
			RainfallCapabilityService capabilityService, RainfallProperties properties, Clock clock) {
		this.observationService = observationService;
		this.cumulativeCalculator = cumulativeCalculator;
		this.intervalCalculator = intervalCalculator;
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
		if (!isSupported(capability)) {
			throw new UnsupportedRainfallStationException(station.getStationId());
		}
		String precipitationKey = capability.precipitationKey();
		if (precipitationKey == null) {
			throw new IllegalStateException("A supported rainfall capability requires a SHEF key");
		}
		Instant queryFrom = (capability.capability() == RainfallCapability.SUPPORTED_ACCUMULATOR)
				? earliest.minus(this.properties.getQuery().getBaselineLookback()) : earliest;
		ObservationBatch observations = this.observationService.observations(station.getStationId(),
				station.getNetwork(), precipitationKey, queryFrom, latest);
		Duration observationPeriod = observationPeriod(capability, observations);
		Map<RainfallWindow, RainfallResult> results = new EnumMap<>(RainfallWindow.class);
		for (Map.Entry<RainfallWindow, RainfallWindow.TimeRange> entry : ranges.entrySet()) {
			RainfallWindow.TimeRange range = entry.getValue();
			results.put(entry.getKey(), calculate(capability.capability(), observations,
					range.from(), range.to(), observationPeriod, unit));
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
		if (!isSupported(capability)) {
			throw new UnsupportedRainfallStationException(station.getStationId());
		}
		String precipitationKey = capability.precipitationKey();
		if (precipitationKey == null) {
			throw new IllegalStateException("A supported rainfall capability requires a SHEF key");
		}
		Instant queryFrom = (capability.capability() == RainfallCapability.SUPPORTED_ACCUMULATOR)
				? from.minus(this.properties.getQuery().getBaselineLookback()) : from;
		ObservationBatch observations = this.observationService.observations(station.getStationId(),
				station.getNetwork(), precipitationKey, queryFrom, to);
		Duration observationPeriod = observationPeriod(capability, observations);
		return calculate(capability.capability(), observations, from, to, observationPeriod, unit);
	}

	/**
	 * Determines the cadence or fixed duration needed by a rainfall calculator.
	 * @param capability the discovered rainfall capability
	 * @param observations the source observations
	 * @return the observation cadence or fixed interval
	 */
	private Duration observationPeriod(
			RainfallCapabilityService.CapabilityDiscovery capability,
			ObservationBatch observations) {
		if (capability.capability() == RainfallCapability.SUPPORTED_ACCUMULATOR) {
			return this.cadenceDetector.detect(observations.observations());
		}
		String precipitationKey = capability.precipitationKey();
		if (precipitationKey == null) {
			throw new IllegalStateException("An interval capability requires a SHEF key");
		}
		return ShefPrecipitationCode.fixedInterval(precipitationKey)
				.orElseThrow(() -> new IllegalStateException(
						"Supported interval capability has an unsupported SHEF duration: "
								+ precipitationKey));
	}

	/**
	 * Delegates a rainfall calculation to the calculator matching the station's
	 * observation semantics.
	 * @param capability the rainfall capability
	 * @param observations the source observation batch
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param observationPeriod the cumulative cadence or interval duration
	 * @param unit the requested rainfall unit
	 * @return the calculated rainfall result
	 */
	private RainfallResult calculate(RainfallCapability capability,
			ObservationBatch observations, Instant from, Instant to, Duration observationPeriod,
			RainfallUnit unit) {
		return switch (capability) {
			case SUPPORTED_ACCUMULATOR -> this.cumulativeCalculator.calculate(
					observations.observations(), from, to, observationPeriod, observations, unit);
			case SUPPORTED_INTERVAL_PRECIPITATION -> this.intervalCalculator.calculate(
					observations.observations(), from, to, observationPeriod, observations, unit);
			default -> throw new IllegalArgumentException(
					"Unsupported rainfall capability " + capability);
		};
	}

	/**
	 * Determines whether the discovered capability can produce public rainfall
	 * totals.
	 * @param discovery the discovered rainfall capability
	 * @return {@code true} when the capability and key are supported
	 */
	private boolean isSupported(RainfallCapabilityService.CapabilityDiscovery discovery) {
		return discovery.precipitationKey() != null
				&& (discovery.capability() == RainfallCapability.SUPPORTED_ACCUMULATOR
						|| discovery.capability()
								== RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION);
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
