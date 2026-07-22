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

package com.pembana.raingauge.observation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes an observation batch.
 * @param observations the precipitation observations to process
 * @param warnings the warnings
 * @param fetchedAt the fetched at
 * @param cacheAge the cache age
 * @param fromCache the from cache
 * @param staleCache the stale cache
 * @param provider the provider
 * @param rejectedRows the rejected rows
 * @author Gunnar Hillert
 */
public record ObservationBatch(
		List<PrecipitationObservation> observations,
		List<String> warnings,
		Instant fetchedAt,
		Duration cacheAge,
		boolean fromCache,
		boolean staleCache,
		String provider,
		int rejectedRows) {

	/**
	 * Creates a new {@code ObservationBatch}.
	 * @param observations the precipitation observations to process
	 * @param warnings the warnings
	 * @param fetchedAt the fetched at
	 * @param cacheAge the cache age
	 * @param fromCache the from cache
	 * @param staleCache the stale cache
	 * @param provider the provider
	 * @param rejectedRows the rejected rows
	 */
	public ObservationBatch {
		observations = List.copyOf(observations);
		warnings = List.copyOf(warnings);
	}

	/**
	 * Returns a copy marked as cache-sourced data.
	 * @param now the current instant
	 * @param stale whether the cached data is stale
	 * @param warning the warning to append to the cached batch
	 * @return the resulting as cached
	 */
	public ObservationBatch asCached(Instant now, boolean stale, String warning) {
		List<String> updatedWarnings = new ArrayList<>(this.warnings);
		if (!warning.isBlank()) {
			updatedWarnings.add(warning);
		}
		return new ObservationBatch(this.observations, updatedWarnings, this.fetchedAt,
				Duration.between(this.fetchedAt, now), true, stale, this.provider, this.rejectedRows);
	}
}
