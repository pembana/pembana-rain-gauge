package com.pembana.raingauge.station;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record StationResponse(
		String stationId,
		String network,
		String sourceName,
		String displayName,
		@Nullable String alias,
		@Nullable String island,
		@Nullable String region,
		@Nullable BigDecimal latitude,
		@Nullable BigDecimal longitude,
		@Nullable BigDecimal elevation,
		boolean online,
		boolean enabled,
		boolean featured,
		boolean catalogConfirmed,
		RainfallCapability rainfallCapability,
		@Nullable String precipitationKey,
		@Nullable Instant latestObservationAt,
		@Nullable LocalDate archiveBegin,
		@Nullable LocalDate archiveEnd,
		@Nullable Instant catalogRefreshedAt,
		@Nullable String note) {

	public static StationResponse from(Station station) {
		return new StationResponse(station.getStationId(), station.getNetwork(),
				station.getSourceName(), station.getDisplayName(), station.getAlias(),
				station.getIsland(), station.getRegion(), station.getLatitude(),
				station.getLongitude(), station.getElevation(), station.isSourceOnline(),
				station.isEnabled(), station.isFeatured(), station.isCatalogConfirmed(),
				station.getRainfallCapability(), station.getPrecipitationKey(),
				station.getLatestObservationAt(), station.getArchiveBegin(), station.getArchiveEnd(),
				station.getCatalogRefreshedAt(), station.getOverrideNote());
	}

	public String selectionLabel() {
		return this.alias == null ? this.displayName + " (" + this.stationId + ')'
				: this.displayName + " (" + this.alias + ')';
	}
}
