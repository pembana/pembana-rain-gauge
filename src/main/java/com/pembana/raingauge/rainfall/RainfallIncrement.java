package com.pembana.raingauge.rainfall;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record RainfallIncrement(
		Instant at,
		BigDecimal inches,
		BigDecimal cumulativeInches,
		@Nullable String qualityFlag) {
}
