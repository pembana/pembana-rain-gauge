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

package com.pembana.raingauge.dashboard;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.support.ProviderStatusRegistry;

/**
 * Describes a dashboard view.
 * @param title the title
 * @param stations the stations
 * @param selectedStation the selected station
 * @param dashboard the dashboard
 * @param period the period
 * @param unit the requested rainfall unit
 * @param error the error
 * @param catalogEmpty the catalog empty
 * @param catalogProvider the catalog provider
 * @param stationMap the station map
 * @author Gunnar Hillert
 */
public record DashboardView(
		String title,
		List<StationResponse> stations,
		@Nullable StationResponse selectedStation,
		@Nullable DashboardResponse dashboard,
		String period,
		String unit,
		@Nullable String error,
		boolean catalogEmpty,
		ProviderStatusRegistry.ProviderState catalogProvider,
		StationMap stationMap) {

	/**
	 * Creates a new {@code DashboardView}.
	 * @param title the title
	 * @param stations the stations
	 * @param selectedStation the selected station
	 * @param dashboard the dashboard
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @param error the error
	 * @param catalogEmpty the catalog empty
	 * @param catalogProvider the catalog provider
	 * @param stationMap the station map
	 */
	public DashboardView {
		stations = List.copyOf(stations);
	}

	/**
	 * Describes a station map.
	 * @param tileUrl the tile URL
	 * @param attributionLabel the attribution label
	 * @param attributionUrl the attribution URL
	 * @author Gunnar Hillert
	 */
	public record StationMap(String tileUrl, String attributionLabel, String attributionUrl) {
	}
}
