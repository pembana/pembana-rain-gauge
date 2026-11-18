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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.pembana.raingauge.cocorahs.DailyReportsResponse;
import com.pembana.raingauge.dashboard.GaugeComparisonService.Comparison;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationType;
import com.pembana.raingauge.support.ProviderStatusRegistry;

/**
 * Describes a dashboard view.
 * @param title the title
 * @param stations the stations
 * @param selectedStation the selected station
 * @param dashboard the dashboard
 * @param reports the selected volunteer station's reports
 * @param reportDates inclusive dates used for volunteer readings
 * @param comparison the optional nearby gauge comparison
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
		@Nullable DailyReportsResponse reports,
		ReportDates reportDates,
		@Nullable Comparison comparison,
		String period,
		String unit,
		@Nullable String error,
		boolean catalogEmpty,
		ProviderStatusRegistry.ProviderState catalogProvider,
		StationMap stationMap) {

	private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("MMM d, uuuu · h:mm a", Locale.US);

	/**
	 * Creates a new {@code DashboardView}.
	 * @param title the title
	 * @param stations the stations
	 * @param selectedStation the selected station
	 * @param dashboard the dashboard
	 * @param reports the selected volunteer station's reports
	 * @param reportDates inclusive dates used for volunteer readings
	 * @param comparison the optional nearby gauge comparison
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

	public boolean dailyStation() {
		return this.selectedStation != null && this.selectedStation.type() == StationType.DAILY_REPORTS;
	}

	public static String readingTime(@Nullable LocalDateTime reading) {
		return (reading != null) ? LOCAL_TIME.format(reading) + " HST" : "No reading in this period";
	}

	public static String observationTime(@Nullable Instant observation) {
		return (observation != null) ? readingTime(observation.atZone(RainfallWindow.HAWAII).toLocalDateTime())
				: "Unavailable";
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
