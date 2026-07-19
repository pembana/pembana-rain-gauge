package com.pembana.raingauge.rainfall;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

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
