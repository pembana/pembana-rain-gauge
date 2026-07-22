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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.pembana.raingauge.observation.ObservationQuality;
import com.pembana.raingauge.observation.PrecipitationObservation;

/**
 * Provides observation cadence detector behavior.
 * @author Gunnar Hillert
 */
@Component
public class ObservationCadenceDetector {

	/** Creates the observation-cadence detector. */
	public ObservationCadenceDetector() {
	}

	private static final Duration DEFAULT_CADENCE = Duration.ofMinutes(15);

	/**
	 * Detects the dominant cadence between valid observations.
	 * @param observations the precipitation observations to process
	 * @return the detected observation cadence
	 */
	public Duration detect(List<PrecipitationObservation> observations) {
		List<Instant> timestamps = observations.stream()
				.filter((observation) -> observation.quality() == ObservationQuality.VALID)
				.map(PrecipitationObservation::validAt)
				.distinct()
				.sorted()
				.toList();
		if (timestamps.size() < 2) {
			return DEFAULT_CADENCE;
		}
		List<Long> intervals = new ArrayList<>();
		for (int index = 1; index < timestamps.size(); index++) {
			long seconds = Duration.between(timestamps.get(index - 1), timestamps.get(index)).toSeconds();
			if (seconds > 0 && seconds <= Duration.ofHours(6).toSeconds()) {
				intervals.add(seconds);
			}
		}
		if (intervals.isEmpty()) {
			return DEFAULT_CADENCE;
		}
		Map<Long, Long> frequencies = new HashMap<>();
		for (Long interval : intervals) {
			frequencies.merge(interval, 1L, Long::sum);
		}
		long mode = frequencies.entrySet().stream()
				.max(Comparator.<Map.Entry<Long, Long>>comparingLong(Map.Entry::getValue)
						.thenComparingLong((entry) -> -entry.getKey()))
				.map(Map.Entry::getKey)
				.orElse(DEFAULT_CADENCE.toSeconds());
		return Duration.ofSeconds(mode);
	}

}
