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
import java.time.Duration;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Describes a rainfall data quality.
 * @param expectedSamples the expected samples
 * @param receivedSamples the received samples
 * @param completenessPercentage the completeness percentage
 * @param longestGap the longest gap
 * @param firstValidObservation the first valid observation
 * @param lastValidObservation the last valid observation
 * @param unresolvedResetCount the unresolved reset count
 * @param recognizedResetCount the recognized reset count
 * @param conflictingObservationCount the conflicting observation count
 * @param parserWarningCount the parser warning count
 * @param sourceAge the source age
 * @param staleCacheUsed the stale cache used
 * @author Gunnar Hillert
 */
public record RainfallDataQuality(
		long expectedSamples,
		long receivedSamples,
		BigDecimal completenessPercentage,
		Duration longestGap,
		@Nullable Instant firstValidObservation,
		@Nullable Instant lastValidObservation,
		int unresolvedResetCount,
		int recognizedResetCount,
		int conflictingObservationCount,
		int parserWarningCount,
		Duration sourceAge,
		boolean staleCacheUsed) {
}
