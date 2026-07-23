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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationService;

/**
 * Handles dashboard API HTTP requests.
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/api/stations/{stationId}")
public class DashboardApiController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final RainfallProperties properties;

	/**
	 * Creates a new {@code DashboardApiController}.
	 * @param stationService the station service
	 * @param dashboardService the dashboard service
	 * @param properties the rainfall application properties
	 */
	public DashboardApiController(StationService stationService, DashboardService dashboardService,
			RainfallProperties properties) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.properties = properties;
	}

	/**
	 * Returns the rainfall dashboard data for a station.
	 * @param stationId the provider station identifier
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @return the dashboard response
	 */
	@GetMapping("/dashboard")
	public DashboardResponse dashboard(@PathVariable String stationId,
			@RequestParam(required = false) @Nullable String period,
			@RequestParam(required = false) @Nullable String unit) {
		validateStationId(stationId);
		Station station = this.stationService.requireRainfallStation(stationId);
		RainfallWindow window = RainfallWindow.fromToken((period != null)
				? period : this.properties.getDashboard().getDefaultPeriod());
		RainfallUnit rainfallUnit = RainfallUnit.fromToken((unit != null)
				? unit : this.properties.getDashboard().getDefaultUnit());
		return this.dashboardService.build(station, window, rainfallUnit);
	}

	/**
	 * Returns rainfall increments for a station.
	 * @param stationId the provider station identifier
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @return the resulting observations
	 */
	@GetMapping("/observations")
	public DashboardResponse.Charts observations(@PathVariable String stationId,
			@RequestParam(defaultValue = "28d") String period,
			@RequestParam(defaultValue = "imperial") String unit) {
		return dashboard(stationId, period, unit).charts();
	}

	/**
	 * Returns rainfall quality events for a station.
	 * @param stationId the provider station identifier
	 * @param period the period
	 * @return the resulting quality events
	 */
	@GetMapping("/quality-events")
	public List<String> qualityEvents(@PathVariable String stationId,
			@RequestParam(defaultValue = "28d") String period) {
		return dashboard(stationId, period, "imperial").warnings();
	}

	/**
	 * Returns monthly rainfall totals for a station.
	 * @param stationId the provider station identifier
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @return the resulting monthly
	 */
	@GetMapping("/monthly")
	public List<DashboardResponse.DailyRainfall> monthly(@PathVariable String stationId,
			@RequestParam(defaultValue = "mtd") String period,
			@RequestParam(defaultValue = "imperial") String unit) {
		return dashboard(stationId, period, unit).dailyRainfall();
	}

	/**
	 * Validates station ID.
	 * @param stationId the provider station identifier
	 */
	private void validateStationId(String stationId) {
		if (!stationId.matches("[A-Za-z0-9]{3,12}")) {
			throw new IllegalArgumentException("Station ID is invalid");
		}
	}

}
