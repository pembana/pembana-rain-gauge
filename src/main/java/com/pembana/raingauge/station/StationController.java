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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.pembana.raingauge.dashboard.DashboardResponse;
import com.pembana.raingauge.dashboard.DashboardService;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallService;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;

/**
 * Handles station HTTP requests.
 * @author Gunnar Hillert
 */
@Controller
public class StationController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final RainfallService rainfallService;

	private final Clock clock;

	/**
	 * Creates a new {@code StationController}.
	 * @param stationService the station service
	 * @param dashboardService the dashboard service
	 * @param rainfallService the rainfall service
	 * @param clock the clock used to obtain the current time
	 */
	public StationController(StationService stationService, DashboardService dashboardService,
			RainfallService rainfallService, Clock clock) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.rainfallService = rainfallService;
		this.clock = clock;
	}

	/**
	 * Returns the public station catalog.
	 * @param query the query
	 * @param island the island
	 * @param online the online
	 * @param capability the capability
	 * @param enabled the enabled
	 * @param recent the recent
	 * @param model the MVC model to populate
	 * @return the resulting stations
	 */
	@GetMapping("/stations")
	public String stations(@RequestParam(required = false) @Nullable String query,
			@RequestParam(required = false) @Nullable String island,
			@RequestParam(required = false) @Nullable Boolean online,
			@RequestParam(required = false) @Nullable RainfallCapability capability,
			@RequestParam(required = false) @Nullable Boolean enabled,
			@RequestParam(required = false) @Nullable Boolean recent, Model model) {
		List<StationResponse> filtered = this.stationService.findAllStations().stream()
				.filter((station) -> matches(station, query, island, online, capability,
						enabled, recent))
				.map(StationResponse::from)
				.toList();
		model.addAttribute("view", new StationListView(filtered, query, island, online,
				capability, enabled, recent, this.stationService.count()));
		return "stations";
	}

	/**
	 * Creates a station for a test scenario.
	 * @param stationId the provider station identifier
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param unit the requested rainfall unit
	 * @param model the MVC model to populate
	 * @return the resulting station
	 */
	@GetMapping("/stations/{stationId}")
	public String station(@PathVariable String stationId,
			@RequestParam(required = false) @Nullable String from,
			@RequestParam(required = false) @Nullable String to,
			@RequestParam(defaultValue = "metric") String unit, Model model) {
		Station station = this.stationService.requirePublicStation(stationId);
		DashboardResponse dashboard = null;
		RainfallResult customResult = null;
		String error = null;
		try {
			dashboard = this.dashboardService.build(station, RainfallWindow.TWENTY_EIGHT_DAYS,
					RainfallUnit.fromToken(unit));
			if (from != null && to != null && !from.isBlank() && !to.isBlank()) {
				customResult = this.rainfallService.calculate(station,
						LocalDateTime.parse(from).atZone(RainfallWindow.HAWAII).toInstant(),
						LocalDateTime.parse(to).atZone(RainfallWindow.HAWAII).toInstant(),
						RainfallUnit.fromToken(unit));
			}
		}
		catch (RuntimeException ex) {
			error = ex.getMessage();
		}
		model.addAttribute("view", new StationDetailView(StationResponse.from(station),
				this.stationService.findRainfallStations().stream().map(StationResponse::from).toList(),
				dashboard, customResult, from, to, unit, error));
		return "stationDetail";
	}

	/**
	 * Determines whether matches.
	 * @param station the station to process
	 * @param query the query
	 * @param island the island
	 * @param online the online
	 * @param capability the capability
	 * @param enabled the enabled
	 * @param recent the recent
	 * @return {@code true} when matches; otherwise {@code false}
	 */
	private boolean matches(Station station, @Nullable String query, @Nullable String island,
			@Nullable Boolean online, @Nullable RainfallCapability capability,
			@Nullable Boolean enabled, @Nullable Boolean recent) {
		if (query != null && !query.isBlank()) {
			String needle = query.toLowerCase(Locale.ROOT);
			if (!station.getStationId().toLowerCase(Locale.ROOT).contains(needle)
					&& !station.getDisplayName().toLowerCase(Locale.ROOT).contains(needle)
					&& !station.getSourceName().toLowerCase(Locale.ROOT).contains(needle)) {
				return false;
			}
		}
		Instant latestObservationAt = station.getLatestObservationAt();
		return (island == null || island.isBlank() || island.equals(station.getIsland()))
				&& (online == null || online == station.isSourceOnline())
				&& (capability == null || capability == station.getRainfallCapability())
				&& (enabled == null || enabled == station.isEnabled())
				&& (recent == null || recent == (latestObservationAt != null
						&& latestObservationAt.isAfter(
								this.clock.instant().minus(Duration.ofHours(24)))));
	}

}
