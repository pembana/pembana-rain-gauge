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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationService;

/**
 * Handles comparison API HTTP requests.
 * @author Gunnar Hillert
 */
@RestController
public class ComparisonApiController {

	private final StationService stationService;

	private final ComparisonService comparisonService;

	/**
	 * Creates a new {@code ComparisonApiController}.
	 * @param stationService the station service
	 * @param comparisonService the comparison service
	 */
	public ComparisonApiController(StationService stationService,
			ComparisonService comparisonService) {
		this.stationService = stationService;
		this.comparisonService = comparisonService;
	}

	/**
	 * Builds the multi-station rainfall comparison.
	 * @param stationIds the station ids
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @return the resulting compare
	 */
	@GetMapping("/api/compare")
	public ComparisonResponse compare(@RequestParam(name = "station") List<String> stationIds,
			@RequestParam(defaultValue = "28d") String period,
			@RequestParam(defaultValue = "metric") String unit) {
		if (stationIds.isEmpty()) {
			throw new IllegalArgumentException("At least one station must be selected");
		}
		List<Station> stations = stationIds.stream()
				.map(this.stationService::requireRainfallStation)
				.toList();
		return this.comparisonService.compare(stations, RainfallWindow.fromToken(period),
				RainfallUnit.fromToken(unit));
	}

}
