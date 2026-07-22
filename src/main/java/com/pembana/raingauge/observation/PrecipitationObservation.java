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

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Describes a precipitation observation.
 * @param stationId the provider station identifier
 * @param validAt the valid at
 * @param receivedAt the received at
 * @param shefKey the SHEF key
 * @param sourceKey the source key
 * @param value the value
 * @param quality the quality
 * @param qualifier the qualifier
 * @param sourceCode the source code
 * @param nativeUnit the native unit
 * @param sourceOrder the source order
 * @author Gunnar Hillert
 */
public record PrecipitationObservation(
		String stationId,
		Instant validAt,
		@Nullable Instant receivedAt,
		String shefKey,
		String sourceKey,
		BigDecimal value,
		ObservationQuality quality,
		@Nullable String qualifier,
		@Nullable String sourceCode,
		@Nullable String nativeUnit,
		int sourceOrder) {

	/**
	 * Creates a valid provider state.
	 * @param stationId the provider station identifier
	 * @param validAt the valid at
	 * @param shefKey the SHEF key
	 * @param value the value
	 * @param sourceOrder the source order
	 * @return the resulting valid
	 */
	public static PrecipitationObservation valid(String stationId, Instant validAt,
			String shefKey, BigDecimal value, int sourceOrder) {
		return new PrecipitationObservation(stationId, validAt, null, shefKey, shefKey,
				value, ObservationQuality.VALID, null, null, "in", sourceOrder);
	}
}
