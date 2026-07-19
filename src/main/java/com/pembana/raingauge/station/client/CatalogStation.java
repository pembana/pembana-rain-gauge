package com.pembana.raingauge.station.client;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record CatalogStation(
		String network,
		String stationId,
		String sourceName,
		@Nullable BigDecimal latitude,
		@Nullable BigDecimal longitude,
		@Nullable BigDecimal elevation,
		@Nullable LocalDate archiveBegin,
		@Nullable LocalDate archiveEnd,
		boolean online,
		@Nullable String state,
		@Nullable String country,
		@Nullable String timeZone,
		@Nullable String sourceMetadata) {
}
