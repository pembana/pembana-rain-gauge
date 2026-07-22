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

import com.pembana.raingauge.dashboard.DashboardResponse;
import com.pembana.raingauge.rainfall.RainfallResult;

import org.jspecify.annotations.Nullable;

/**
 * Describes a station detail view.
 * @param station the station to process
 * @param stations the stations
 * @param dashboard the dashboard
 * @param customResult the custom result
 * @param from the inclusive start of the requested interval
 * @param to the exclusive end of the requested interval
 * @param unit the requested rainfall unit
 * @param error the error
 * @author Gunnar Hillert
 */
public record StationDetailView(
		StationResponse station,
		List<StationResponse> stations,
		@Nullable DashboardResponse dashboard,
		@Nullable RainfallResult customResult,
		@Nullable String from,
		@Nullable String to,
		String unit,
		@Nullable String error) {

	/**
	 * Creates a new {@code StationDetailView}.
	 * @param station the station to process
	 * @param stations the stations
	 * @param dashboard the dashboard
	 * @param customResult the custom result
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param unit the requested rainfall unit
	 * @param error the error
	 */
	public StationDetailView {
		stations = List.copyOf(stations);
	}
}
