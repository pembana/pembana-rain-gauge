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

package com.pembana.raingauge.observation.shef;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Interprets precipitation-specific parts of an expanded SHEF parameter code.
 * @author Gunnar Hillert
 */
public final class ShefPrecipitationCode {

	private static final Map<Character, Duration> FIXED_DURATIONS = Map.ofEntries(
			Map.entry('U', Duration.ofMinutes(1)),
			Map.entry('E', Duration.ofMinutes(5)),
			Map.entry('G', Duration.ofMinutes(10)),
			Map.entry('C', Duration.ofMinutes(15)),
			Map.entry('J', Duration.ofMinutes(30)),
			Map.entry('H', Duration.ofHours(1)),
			Map.entry('B', Duration.ofHours(2)),
			Map.entry('T', Duration.ofHours(3)),
			Map.entry('F', Duration.ofHours(4)),
			Map.entry('Q', Duration.ofHours(6)),
			Map.entry('A', Duration.ofHours(8)),
			Map.entry('K', Duration.ofHours(12)),
			Map.entry('L', Duration.ofHours(18)),
			Map.entry('D', Duration.ofDays(1)),
			Map.entry('W', Duration.ofDays(7)));

	private ShefPrecipitationCode() {
	}

	/**
	 * Returns the fixed interval represented by an interval-precipitation code.
	 * <p>
	 * A missing or filler duration uses the SHEF default of one day for {@code PP}.
	 * Calendar, variable, seasonal, period-of-record, and unknown durations cannot be
	 * represented by a fixed {@link Duration} and are therefore not returned.
	 * @param shefKey the normalized or expanded SHEF parameter code
	 * @return the fixed interval, or an empty result when the code is not a supported
	 * interval-precipitation code
	 */
	public static Optional<Duration> fixedInterval(String shefKey) {
		String normalized = normalize(shefKey);
		if (!normalized.startsWith("PP")) {
			return Optional.empty();
		}
		if (normalized.length() == 2 || normalized.charAt(2) == 'Z') {
			return Optional.of(Duration.ofDays(1));
		}
		return Optional.ofNullable(FIXED_DURATIONS.get(normalized.charAt(2)));
	}

	/**
	 * Normalizes a SHEF parameter code for interpretation.
	 * @param shefKey the SHEF parameter code
	 * @return the normalized uppercase code without trailing filler characters
	 */
	private static String normalize(String shefKey) {
		String normalized = shefKey.strip().toUpperCase(java.util.Locale.ROOT);
		return normalized.endsWith("ZZ")
				? normalized.substring(0, normalized.length() - 2) : normalized;
	}

}
