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

package com.pembana.raingauge.station;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * Describes a station response.
 * @param stationId the provider station identifier
 * @param network the provider network identifier
 * @param sourceName the source name
 * @param displayName the display name
 * @param alias the alias
 * @param island the island
 * @param region the region
 * @param latitude the latitude
 * @param longitude the longitude
 * @param elevation the elevation
 * @param online the online
 * @param enabled the enabled
 * @param featured the featured
 * @param catalogConfirmed the catalog confirmed
 * @param rainfallCapability the rainfall capability
 * @param precipitationKey the precipitation key
 * @param latestObservationAt the latest observation timestamp
 * @param archiveBegin the archive begin
 * @param archiveEnd the archive end
 * @param catalogRefreshedAt the catalog refreshed at
 * @param note the note
 * @author Gunnar Hillert
 */
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

	/**
	 * Creates a station response from a station entity.
	 * @param station the station to process
	 * @return the value represented by the supplied token or domain object
	 */
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

	/**
	 * Returns the station label used by selection controls.
	 * @return the resulting selection label
	 */
	public String selectionLabel() {
		return (this.alias != null) ? this.displayName + " (" + this.alias + ')'
				: this.displayName + " (" + this.stationId + ')';
	}
}
