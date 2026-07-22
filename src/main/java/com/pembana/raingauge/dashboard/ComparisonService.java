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

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;

/**
 * Provides comparison operations.
 * @author Gunnar Hillert
 */
@Service
public class ComparisonService {

	private final DashboardService dashboardService;

	/**
	 * Creates a new {@code ComparisonService}.
	 * @param dashboardService the dashboard service
	 */
	public ComparisonService(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	/**
	 * Builds the multi-station rainfall comparison.
	 * @param stations the stations
	 * @param window the requested rainfall window
	 * @param unit the requested rainfall unit
	 * @return the resulting compare
	 */
	public ComparisonResponse compare(List<Station> stations, RainfallWindow window,
			RainfallUnit unit) {
		if (stations.size() > 8) {
			throw new IllegalArgumentException("No more than eight stations can be compared");
		}
		List<ComparisonResponse.ComparisonStation> results = new ArrayList<>();
		for (Station station : stations) {
			DashboardResponse dashboard = this.dashboardService.build(station, window, unit);
			results.add(new ComparisonResponse.ComparisonStation(dashboard.station(),
					selectedTotal(dashboard, window), dashboard.dailyRainfall(),
					dashboard.charts().cumulative()));
		}
		return new ComparisonResponse(window.token(), unit.token(), results);
	}

	/**
	 * Returns the total for the selected rainfall window.
	 * @param dashboard the dashboard
	 * @param window the requested rainfall window
	 * @return the resulting selected total
	 */
	private DashboardResponse.Result selectedTotal(DashboardResponse dashboard,
			RainfallWindow window) {
		DashboardResponse.Summary summary = dashboard.summary();
		return switch (window) {
			case ONE_HOUR -> summary.oneHour();
			case THREE_HOURS -> summary.threeHours();
			case SIX_HOURS -> summary.sixHours();
			case TWELVE_HOURS -> summary.twelveHours();
			case TWENTY_FOUR_HOURS -> summary.twentyFourHours();
			case SEVEN_DAYS -> summary.sevenDays();
			case TWENTY_EIGHT_DAYS -> summary.twentyEightDays();
			case MONTH_TO_DATE, CALENDAR_MONTH -> summary.monthToDate();
			case YEAR_TO_DATE -> summary.yearToDate();
			case PREVIOUS_CALENDAR_YEAR -> throw new IllegalArgumentException(
					"Previous-year comparison is not available in the summary response");
		};
	}

}
