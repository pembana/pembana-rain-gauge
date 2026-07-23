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

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationService;

/**
 * Handles comparison HTTP requests.
 * @author Gunnar Hillert
 */
@Controller
public class ComparisonController {

	private final StationService stationService;

	private final ComparisonService comparisonService;

	/**
	 * Creates a new {@code ComparisonController}.
	 * @param stationService the station service
	 * @param comparisonService the comparison service
	 */
	public ComparisonController(StationService stationService, ComparisonService comparisonService) {
		this.stationService = stationService;
		this.comparisonService = comparisonService;
	}

	/**
	 * Builds the multi-station rainfall comparison.
	 * @param stationIds the station ids
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @param model the MVC model to populate
	 * @return the resulting compare
	 */
	@GetMapping("/compare")
	public String compare(@RequestParam(name = "station", required = false)
			@Nullable List<String> stationIds,
			@RequestParam(defaultValue = "28d") String period,
			@RequestParam(defaultValue = "metric") String unit, Model model) {
		List<Station> available = this.stationService.findRainfallStations();
		List<String> selectedIds = (stationIds != null) ? stationIds : List.of();
		ComparisonResponse response = null;
		String error = null;
		if (!selectedIds.isEmpty()) {
			try {
				List<Station> selected = new ArrayList<>();
				for (String stationId : selectedIds) {
					selected.add(this.stationService.requireRainfallStation(stationId));
				}
				response = this.comparisonService.compare(selected, RainfallWindow.fromToken(period),
						RainfallUnit.fromToken(unit));
			}
			catch (RuntimeException ex) {
				error = ex.getMessage();
			}
		}
		model.addAttribute("view", new ComparisonView(
				available.stream().map(StationResponse::from).toList(), selectedIds, period, unit,
				response, error));
		return "compare";
	}

}
