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
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.json.JsonMapper;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.UnsupportedRainfallStationException;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationNotFoundException;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationService;

/**
 * Renders the year comparison and its accessible rainfall tables.
 * @author OpenAI Codex
 */
@Controller
public class YearComparisonController {

	private final StationService stations;

	private final YearComparisonService comparison;

	private final JsonMapper jsonMapper;

	private final String defaultStation;

	YearComparisonController(StationService stations, YearComparisonService comparison,
			JsonMapper jsonMapper, RainfallProperties properties) {
		this.stations = stations;
		this.comparison = comparison;
		this.jsonMapper = jsonMapper;
		this.defaultStation = properties.getDashboard().getDefaultStation();
	}

	@GetMapping("/compare/years")
	String compare(@RequestParam(name = "station", required = false) @Nullable String stationId,
			@RequestParam(name = "year", required = false) @Nullable List<String> years,
			@RequestParam(defaultValue = "calendar") String mode,
			@RequestParam(defaultValue = "metric") String unit, Model model) {
		List<Station> stations = this.stations.findRainfallStations();
		Station selected = null;
		List<Integer> availableYears = List.of();
		List<Integer> selectedYears = List.of();
		YearComparisonResponse response = null;
		String error = null;
		try {
			selected = select(stations, stationId);
			if (selected != null) {
				availableYears = this.comparison.availableYears(selected).reversed();
				selectedYears = (years != null)
						? years.stream().map(Integer::valueOf).toList() : List.of();
				response = this.comparison.compare(selected, selectedYears, mode,
						RainfallUnit.fromToken(unit));
				selectedYears = response.years().stream().map(YearComparisonResponse.YearResult::year)
					.toList();
			}
		}
		catch (NumberFormatException ex) {
			error = "Choose valid calendar years";
		}
		catch (IllegalArgumentException | StationNotFoundException
				| UnsupportedRainfallStationException ex) {
			error = ex.getMessage();
		}

		model.addAttribute("stations", stations.stream().map(StationResponse::from).toList());
		model.addAttribute("stationId", (selected != null) ? selected.getStationId() : "");
		model.addAttribute("availableYears", availableYears);
		model.addAttribute("selectedYears", selectedYears);
		model.addAttribute("unit", unit.toLowerCase(Locale.ROOT));
		model.addAttribute("mode", mode);
		model.addAttribute("comparison", response);
		model.addAttribute("chartData", (response != null)
				? this.jsonMapper.writeValueAsString(response) : "");
		model.addAttribute("error", error);
		return "compareYears";
	}

	private @Nullable Station select(List<Station> available, @Nullable String requested) {
		if (requested != null) {
			return this.stations.requireRainfallStation(requested);
		}
		return available.stream().filter((station) -> station.getStationId()
				.equalsIgnoreCase(this.defaultStation)).findFirst()
			.orElseGet(() -> available.isEmpty() ? null : available.getFirst());
	}

}
