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

import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pembana.raingauge.cocorahs.DailyReportService;
import com.pembana.raingauge.cocorahs.DailyReportsResponse;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationService;
import com.pembana.raingauge.station.StationType;
import com.pembana.raingauge.support.ProviderStatusRegistry;

/**
 * Handles dashboard HTTP requests.
 * @author Gunnar Hillert
 */
@Controller
public class DashboardController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final DailyReportService dailyReports;

	private final GaugeComparisonService comparisons;

	private final RainfallProperties properties;

	private final ProviderStatusRegistry providerStatusRegistry;

	/**
	 * Creates a new {@code DashboardController}.
	 * @param stationService the station service
	 * @param dashboardService the dashboard service
	 * @param dailyReports the volunteer reports service
	 * @param comparisons the reading-window comparison service
	 * @param properties the rainfall application properties
	 * @param providerStatusRegistry the provider status registry
	 */
	public DashboardController(StationService stationService, DashboardService dashboardService,
			DailyReportService dailyReports, GaugeComparisonService comparisons,
			RainfallProperties properties, ProviderStatusRegistry providerStatusRegistry) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.dailyReports = dailyReports;
		this.comparisons = comparisons;
		this.properties = properties;
		this.providerStatusRegistry = providerStatusRegistry;
	}

	/**
	 * Renders the rainfall dashboard for the selected station and period.
	 * @param station the station to process
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @param compare optional counterpart station ID or nearest
	 * @param model the MVC model to populate
	 * @return the dashboard template name
	 */
	@GetMapping("/")
	public String dashboard(@RequestParam(required = false) @Nullable String station,
			@RequestParam(required = false) @Nullable String period,
			@RequestParam(required = false) @Nullable String unit,
			@RequestParam(required = false) @Nullable String compare, Model model) {
		List<Station> stations = this.stationService.findHistoricalRainfallStations();
		String periodToken = (period != null) ? period : this.properties.getDashboard().getDefaultPeriod();
		String unitToken = (unit != null) ? unit : this.properties.getDashboard().getDefaultUnit();
		RainfallWindow window = RainfallWindow.fromToken(periodToken);
		RainfallUnit rainfallUnit = RainfallUnit.fromToken(unitToken);
		Station selected = select(stations, station);
		if (selected != null && selected.getType() == StationType.DAILY_REPORTS) {
			window = switch (window) {
				case ONE_HOUR, THREE_HOURS, SIX_HOURS, TWELVE_HOURS -> RainfallWindow.TWENTY_FOUR_HOURS;
				default -> window;
			};
		}
		ReportDates dates = ReportDates.forWindow(window, this.dailyReports.today());
		DashboardResponse response = null;
		DailyReportsResponse reports = null;
		GaugeComparisonService.Comparison comparison = null;
		String error = null;
		if (selected != null) {
			try {
				if (selected.getType() == StationType.DAILY_REPORTS) {
					reports = this.dailyReports.load(selected, dates.from(), dates.to(), rainfallUnit, false);
				}
				else {
					response = this.dashboardService.build(selected, window, rainfallUnit);
				}
			}
			catch (RuntimeException ex) {
				error = (selected.getType() == StationType.DAILY_REPORTS)
						? "CoCoRaHS reports are temporarily unavailable." : "Automated observations are temporarily unavailable.";
			}
			if (compare != null && !compare.isBlank()) {
				comparison = this.comparisons.compare(selected, stations, compare, dates, rainfallUnit, reports);
			}
		}
		DashboardView view = new DashboardView(
				(selected != null) ? selected.getDisplayName() + " rainfall — Pembana Rain Gauge"
						: "Pembana Rain Gauge — Hawaiʻi Rainfall Station Data",
				stations.stream().map(StationResponse::from).toList(),
				(selected != null) ? StationResponse.from(selected) : null, response, reports, dates, comparison,
				window.token(), rainfallUnit.token(), error, stations.isEmpty(), this.providerStatusRegistry.catalog(),
				new DashboardView.StationMap(this.properties.getStationMap().getTileUrl(),
						this.properties.getStationMap().getAttributionLabel(),
						this.properties.getStationMap().getAttributionUrl()));
		model.addAttribute("view", view);
		return "dashboard";
	}

	/**
	 * Selects the requested station or the first available station.
	 * @param stations the stations
	 * @param requested the requested
	 * @return the resulting select
	 */
	private @Nullable Station select(List<Station> stations, @Nullable String requested) {
		if (stations.isEmpty()) {
			return null;
		}
		String stationId = (requested != null)
				? requested : this.properties.getDashboard().getDefaultStation();
		return stations.stream()
				.filter((station) -> station.getStationId().equalsIgnoreCase(stationId))
				.findFirst()
				.orElseGet(() -> stations.stream().min(Comparator.comparing(Station::isFeatured).reversed()
						.thenComparing(Station::getType)
						.thenComparing((candidate) -> candidate.getArchiveEnd() != null)).orElseThrow());
	}

}
