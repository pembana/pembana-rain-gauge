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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.station.StationResponse;

/**
 * CoCoRaHS precipitation for an inclusive range of Hawaiʻi observation dates.
 * @param station the station metadata
 * @param from the first observation date
 * @param to the last observation date
 * @param unit the display unit
 * @param periodBasis the basis for date ranges
 * @param days the daily series, including gaps
 * @param spans the multi-day accumulations
 * @param latestReport the station's latest report, when known
 * @param total the measurable precipitation in the selected unit
 * @param display the formatted total
 * @param trace whether included reports contain trace precipitation
 * @param coverage the number of usable report dates
 * @param status the quality of the total
 * @param source the provider and cache metadata
 * @param warnings notes about the selected data
 * @author OpenAI Codex
 */
public record DailyReportsResponse(StationResponse station, LocalDate from, LocalDate to,
		String unit, String periodBasis, List<Day> days, List<Span> spans,
		@Nullable DailyReport latestReport, @Nullable BigDecimal total, String display,
		boolean trace, Coverage coverage, RainfallResultStatus status, Source source,
		List<String> warnings) {

	public record Day(LocalDate date, @Nullable DailyReport report, @Nullable BigDecimal value,
			String display) {
	}

	public record Span(DailyReport report, @Nullable BigDecimal value, String display,
			boolean included, String reason) {
	}

	public record Coverage(int expectedDays, int dailyReportDays, int coveredDays) {
	}

	public record Source(String provider, String catalogProvider, @Nullable Instant fetchedAt, boolean staleCache) {
	}

}
