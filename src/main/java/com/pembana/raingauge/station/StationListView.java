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

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Describes a station list view.
 * @param stations the stations
 * @param query the query
 * @param island the island
 * @param online the online
 * @param capability the capability
 * @param enabled the enabled
 * @param recent the recent
 * @param totalStations the total stations
 * @author Gunnar Hillert
 */
public record StationListView(
		List<StationResponse> stations,
		@Nullable String query,
		@Nullable String island,
		@Nullable Boolean online,
		@Nullable RainfallCapability capability,
		@Nullable Boolean enabled,
		@Nullable Boolean recent,
		long totalStations) {

	/**
	 * Creates a new {@code StationListView}.
	 * @param stations the stations
	 * @param query the query
	 * @param island the island
	 * @param online the online
	 * @param capability the capability
	 * @param enabled the enabled
	 * @param recent the recent
	 * @param totalStations the total stations
	 */
	public StationListView {
		stations = List.copyOf(stations);
	}
}
