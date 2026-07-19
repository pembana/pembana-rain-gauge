package com.pembana.raingauge.observation;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

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

	public static PrecipitationObservation valid(String stationId, Instant validAt,
			String shefKey, BigDecimal value, int sourceOrder) {
		return new PrecipitationObservation(stationId, validAt, null, shefKey, shefKey,
				value, ObservationQuality.VALID, null, null, "in", sourceOrder);
	}
}
