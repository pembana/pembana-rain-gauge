package com.pembana.raingauge.rainfall;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

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

	public RainfallResult {
		warnings = List.copyOf(warnings);
		increments = List.copyOf(increments);
	}

	public String displayValue() {
		return this.amount == null ? "Unavailable" : this.amount.display(this.unit);
	}
}
