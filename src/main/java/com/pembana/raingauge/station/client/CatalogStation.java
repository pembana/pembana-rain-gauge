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

package com.pembana.raingauge.station.client;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * Describes a catalog station.
 * @param network the provider network identifier
 * @param stationId the provider station identifier
 * @param sourceName the source name
 * @param latitude the latitude
 * @param longitude the longitude
 * @param elevation the elevation
 * @param archiveBegin the archive begin
 * @param archiveEnd the archive end
 * @param online the online
 * @param state the state
 * @param country the country
 * @param timeZone the time zone
 * @param sourceMetadata the source metadata
 * @author Gunnar Hillert
 */
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
