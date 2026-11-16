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

package com.pembana.raingauge.cocorahs;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.UnsupportedRainfallStationException;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationService;
import com.pembana.raingauge.station.StationType;

/**
 * Exposes CoCoRaHS reports and reporting coverage for a public station.
 * @author OpenAI Codex
 */
@RestController
class DailyReportApiController {

	private final StationService stations;

	private final DailyReportService reports;

	DailyReportApiController(StationService stations, DailyReportService reports) {
		this.stations = stations;
		this.reports = reports;
	}

	@GetMapping("/api/stations/{stationId}/daily-reports")
	DailyReportsResponse reports(@PathVariable String stationId,
			@RequestParam(required = false) @Nullable LocalDate from,
			@RequestParam(required = false) @Nullable LocalDate to,
			@RequestParam(defaultValue = "metric") String unit) {
		Station station = this.stations.requirePublicStation(stationId);
		if (station.getType() != StationType.DAILY_REPORTS) {
			throw new UnsupportedRainfallStationException(stationId);
		}
		LocalDate end = (to != null) ? to : this.reports.today();
		LocalDate start = (from != null) ? from : end.minusDays(27);
		return this.reports.load(station, start, end, RainfallUnit.fromToken(unit), true);
	}

}
