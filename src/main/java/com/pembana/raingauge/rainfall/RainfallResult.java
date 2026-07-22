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
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Describes a rainfall result.
 * @param amount the amount
 * @param unit the requested rainfall unit
 * @param nativeUnit the native unit
 * @param displayScale the display scale
 * @param sourceResolution the source resolution
 * @param requestedStart the requested start
 * @param requestedEnd the requested end
 * @param coveredStart the covered start
 * @param coveredEnd the covered end
 * @param observationCutoff the observation cutoff
 * @param calculatedAt the calculated at
 * @param status the status
 * @param warnings the warnings
 * @param quality the quality
 * @param increments the increments
 * @param provider the provider
 * @param fetchedAt the fetched at
 * @author Gunnar Hillert
 */
public record RainfallResult(
		@Nullable RainfallAmount amount,
		RainfallUnit unit,
		String nativeUnit,
		int displayScale,
		BigDecimal sourceResolution,
		Instant requestedStart,
		Instant requestedEnd,
		@Nullable Instant coveredStart,
		@Nullable Instant coveredEnd,
		@Nullable Instant observationCutoff,
		Instant calculatedAt,
		RainfallResultStatus status,
		List<String> warnings,
		RainfallDataQuality quality,
		List<RainfallIncrement> increments,
		String provider,
		Instant fetchedAt) {

	/**
	 * Creates a new {@code RainfallResult}.
	 * @param amount the amount
	 * @param unit the requested rainfall unit
	 * @param nativeUnit the native unit
	 * @param displayScale the display scale
	 * @param sourceResolution the source resolution
	 * @param requestedStart the requested start
	 * @param requestedEnd the requested end
	 * @param coveredStart the covered start
	 * @param coveredEnd the covered end
	 * @param observationCutoff the observation cutoff
	 * @param calculatedAt the calculated at
	 * @param status the status
	 * @param warnings the warnings
	 * @param quality the quality
	 * @param increments the increments
	 * @param provider the provider
	 * @param fetchedAt the fetched at
	 */
	public RainfallResult {
		warnings = List.copyOf(warnings);
		increments = List.copyOf(increments);
	}

	/**
	 * Formats the rainfall result for display.
	 * @return the formatted display value
	 */
	public String displayValue() {
		return this.amount == null ? "Unavailable" : this.amount.display(this.unit);
	}
}
