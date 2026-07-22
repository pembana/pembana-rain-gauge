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

import com.pembana.raingauge.station.StationResponse;

/**
 * Describes a comparison response.
 * @param period the period
 * @param unit the requested rainfall unit
 * @param stations the stations
 * @author Gunnar Hillert
 */
public record ComparisonResponse(String period, String unit, List<ComparisonStation> stations) {

	/**
	 * Creates a new {@code ComparisonResponse}.
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @param stations the stations
	 */
	public ComparisonResponse {
		stations = List.copyOf(stations);
	}

	/**
	 * Describes a comparison station.
	 * @param station the station to process
	 * @param total the total
	 * @param dailyRainfall the daily rainfall
	 * @param cumulativeRainfall the cumulative rainfall
	 * @author Gunnar Hillert
	 */
	public record ComparisonStation(StationResponse station, DashboardResponse.Result total,
			List<DashboardResponse.DailyRainfall> dailyRainfall,
			List<DashboardResponse.ChartPoint> cumulativeRainfall) {

		/**
		 * Creates a new {@code ComparisonStation}.
		 * @param station the station to process
		 * @param total the total
		 * @param dailyRainfall the daily rainfall
		 * @param cumulativeRainfall the cumulative rainfall
		 */
		public ComparisonStation {
			dailyRainfall = List.copyOf(dailyRainfall);
			cumulativeRainfall = List.copyOf(cumulativeRainfall);
		}
	}
}
