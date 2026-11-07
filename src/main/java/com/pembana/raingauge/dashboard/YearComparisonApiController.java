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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.station.StationService;

/**
 * Returns station rainfall totals and chart data across selected years.
 * @author OpenAI Codex
 */
@RestController
public class YearComparisonApiController {

	private final StationService stations;

	private final YearComparisonService comparison;

	YearComparisonApiController(StationService stations, YearComparisonService comparison) {
		this.stations = stations;
		this.comparison = comparison;
	}

	@GetMapping("/api/stations/{stationId}/years")
	YearComparisonResponse compare(@PathVariable String stationId,
			@RequestParam(name = "year", required = false) @Nullable List<Integer> years,
			@RequestParam(defaultValue = "calendar") String mode,
			@RequestParam(defaultValue = "metric") String unit) {
		return this.comparison.compare(this.stations.requireRainfallStation(stationId),
				(years != null) ? years : List.of(), mode, RainfallUnit.fromToken(unit));
	}

}
