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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationService;
import com.pembana.raingauge.support.ProviderStatusRegistry;

/**
 * Handles dashboard HTTP requests.
 * @author Gunnar Hillert
 */
@Controller
public class DashboardController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final RainfallProperties properties;

	private final ProviderStatusRegistry providerStatusRegistry;

	/**
	 * Creates a new {@code DashboardController}.
	 * @param stationService the station service
	 * @param dashboardService the dashboard service
	 * @param properties the rainfall application properties
	 * @param providerStatusRegistry the provider status registry
	 */
	public DashboardController(StationService stationService, DashboardService dashboardService,
			RainfallProperties properties, ProviderStatusRegistry providerStatusRegistry) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.properties = properties;
		this.providerStatusRegistry = providerStatusRegistry;
	}

	/**
	 * Renders the rainfall dashboard for the selected station and period.
	 * @param station the station to process
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @param model the MVC model to populate
	 * @return the dashboard template name
	 */
	@GetMapping("/")
	public String dashboard(@RequestParam(required = false) @Nullable String station,
			@RequestParam(required = false) @Nullable String period,
			@RequestParam(required = false) @Nullable String unit, Model model) {
		List<Station> stations = this.stationService.findRainfallStations();
		String periodToken = (period != null) ? period : this.properties.getDashboard().getDefaultPeriod();
		String unitToken = (unit != null) ? unit : this.properties.getDashboard().getDefaultUnit();
		RainfallWindow window = RainfallWindow.fromToken(periodToken);
		RainfallUnit rainfallUnit = RainfallUnit.fromToken(unitToken);
		Station selected = select(stations, station);
		DashboardResponse response = null;
		String error = null;
		if (selected != null) {
			try {
				response = this.dashboardService.build(selected, window, rainfallUnit);
			}
			catch (RuntimeException ex) {
				error = "Rainfall observations are temporarily unavailable: " + ex.getMessage();
			}
		}
		DashboardView view = new DashboardView(
				(selected != null) ? selected.getDisplayName() + " rainfall — Pembana Rain Gauge"
						: "Pembana Rain Gauge — Hawaiʻi Rainfall Station Data",
				stations.stream().map(StationResponse::from).toList(),
				(selected != null) ? StationResponse.from(selected) : null, response, periodToken,
				unitToken, error, stations.isEmpty(), this.providerStatusRegistry.catalog(),
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
				.orElseGet(() -> stations.stream().filter(Station::isFeatured).findFirst()
						.orElse(stations.getFirst()));
	}

}
