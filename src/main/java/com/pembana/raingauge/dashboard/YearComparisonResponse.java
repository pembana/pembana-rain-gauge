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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.station.StationResponse;

/**
 * Rainfall comparisons for a station across calendar years.
 * @param station the selected station
 * @param unit the display unit token
 * @param mode the comparison mode
 * @param years the selected years in chronological order
 * @author OpenAI Codex
 */
public record YearComparisonResponse(StationResponse station, String unit, String mode,
		List<YearResult> years) {

	/**
	 * Rainfall for one selected period. Period boundaries use Hawaiʻi local time.
	 * @param year the calendar year
	 * @param from the inclusive start
	 * @param to the exclusive end
	 * @param yearToDate whether the period ends before the next calendar year
	 * @param value the total in the requested unit
	 * @param display the formatted total
	 * @param status the quality status
	 * @param completeness the percentage of expected samples received
	 * @param observationCutoff the latest observation used
	 * @param provider the observation source
	 * @param fetchedAt the source fetch time
	 * @param staleCache whether an older cached response was used
	 * @param warnings the calculation and provider warnings
	 * @param months the monthly rainfall buckets
	 * @param cumulative the daily cumulative rainfall points
	 */
	public record YearResult(int year, LocalDateTime from, LocalDateTime to, boolean yearToDate,
			@Nullable BigDecimal value, String display, RainfallResultStatus status,
			@Nullable BigDecimal completeness, @Nullable Instant observationCutoff,
			@Nullable String provider, @Nullable Instant fetchedAt, boolean staleCache,
			List<String> warnings, List<MonthResult> months, List<DailyPoint> cumulative) {

	}

	/**
	 * A month with measured rainfall, or an empty bucket.
	 * @param month the month number
	 * @param value the rainfall in the requested unit
	 * @param display the formatted rainfall
	 */
	public record MonthResult(int month, @Nullable BigDecimal value, String display) {

	}

	/**
	 * Observed accumulation through a given day.
	 * @param date the Hawaiʻi local date
	 * @param value the cumulative rainfall, or null when the day has no calculated rainfall
	 */
	public record DailyPoint(LocalDate date, @Nullable BigDecimal value) {

	}

}
