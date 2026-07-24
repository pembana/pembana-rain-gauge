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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.pembana.raingauge.observation.ObservationQuality;
import com.pembana.raingauge.observation.PrecipitationObservation;

/**
 * Deterministically collapses precipitation observation retransmissions.
 * @author Gunnar Hillert
 */
public final class PrecipitationObservationDeduplicator {

	private PrecipitationObservationDeduplicator() {
	}

	/**
	 * Deduplicates observations by timestamp, preferring valid and earlier source
	 * rows while preserving conflicts as quality information.
	 * @param source the source observations
	 * @return the deduplicated observations and conflict information
	 */
	public static Result deduplicate(List<PrecipitationObservation> source) {
		Map<Instant, List<PrecipitationObservation>> grouped = new TreeMap<>();
		for (PrecipitationObservation observation : source) {
			grouped.computeIfAbsent(observation.validAt(), (ignored) -> new ArrayList<>())
					.add(observation);
		}
		List<PrecipitationObservation> observations = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		int conflicts = 0;
		for (Map.Entry<Instant, List<PrecipitationObservation>> entry : grouped.entrySet()) {
			Map<BigDecimal, List<PrecipitationObservation>> byValue = new LinkedHashMap<>();
			for (PrecipitationObservation observation : entry.getValue()) {
				byValue.computeIfAbsent(observation.value().stripTrailingZeros(),
						(ignored) -> new ArrayList<>()).add(observation);
			}
			if (byValue.size() > 1) {
				conflicts++;
				warnings.add("Conflicting retransmissions at " + entry.getKey());
			}
			PrecipitationObservation selected = entry.getValue().stream()
					.sorted(Comparator
							.comparingInt(PrecipitationObservationDeduplicator::qualityRank)
							.thenComparingInt(PrecipitationObservation::sourceOrder))
					.findFirst()
					.orElseThrow();
			observations.add(selected);
		}
		return new Result(observations, warnings, conflicts);
	}

	/**
	 * Returns the deterministic selection rank for an observation's quality.
	 * @param observation the observation
	 * @return the selection rank
	 */
	private static int qualityRank(PrecipitationObservation observation) {
		return (observation.quality() == ObservationQuality.VALID) ? 0 : 1;
	}

	/**
	 * Contains deduplicated observations and retransmission warnings.
	 * @param observations the deduplicated observations
	 * @param warnings the retransmission warnings
	 * @param conflicts the number of conflicting timestamps
	 * @author Gunnar Hillert
	 */
	public record Result(List<PrecipitationObservation> observations, List<String> warnings,
			int conflicts) {

		/**
		 * Creates an immutable deduplication result.
		 * @param observations the deduplicated observations
		 * @param warnings the retransmission warnings
		 * @param conflicts the number of conflicting timestamps
		 */
		public Result {
			observations = List.copyOf(observations);
			warnings = List.copyOf(warnings);
		}
	}

}
