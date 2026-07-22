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
import java.time.ZonedDateTime;
import java.util.List;

import com.pembana.raingauge.rainfall.RainfallDataQuality;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.station.StationResponse;

import org.jspecify.annotations.Nullable;

/**
 * Describes a dashboard response.
 * @param station the station to process
 * @param selection the selection
 * @param calculatedAt the calculated at
 * @param observationCutoff the observation cutoff
 * @param summary the summary
 * @param quality the quality
 * @param dailyRainfall the daily rainfall
 * @param charts the charts
 * @param source the source data
 * @param discrepancies the discrepancies
 * @param warnings the warnings
 * @author Gunnar Hillert
 */
public record DashboardResponse(
		StationResponse station,
		Selection selection,
		Instant calculatedAt,
		@Nullable Instant observationCutoff,
		Summary summary,
		RainfallDataQuality quality,
		List<DailyRainfall> dailyRainfall,
		Charts charts,
		Source source,
		List<SourceDiscrepancy> discrepancies,
		List<String> warnings) {

	/**
	 * Creates a new {@code DashboardResponse}.
	 * @param station the station to process
	 * @param selection the selection
	 * @param calculatedAt the calculated at
	 * @param observationCutoff the observation cutoff
	 * @param summary the summary
	 * @param quality the quality
	 * @param dailyRainfall the daily rainfall
	 * @param charts the charts
	 * @param source the source data
	 * @param discrepancies the discrepancies
	 * @param warnings the warnings
	 */
	public DashboardResponse {
		dailyRainfall = List.copyOf(dailyRainfall);
		discrepancies = List.copyOf(discrepancies);
		warnings = List.copyOf(warnings);
	}

	/**
	 * Describes a selection.
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @author Gunnar Hillert
	 */
	public record Selection(String period, String unit, ZonedDateTime from, ZonedDateTime to) {
	}

	/**
	 * Describes a summary.
	 * @param oneHour the one hour
	 * @param threeHours the three hours
	 * @param sixHours the six hours
	 * @param twelveHours the twelve hours
	 * @param twentyFourHours the twenty four hours
	 * @param sevenDays the seven days
	 * @param twentyEightDays the twenty eight days
	 * @param monthToDate the month to date
	 * @param yearToDate the year to date
	 * @author Gunnar Hillert
	 */
	public record Summary(
			Result oneHour,
			Result threeHours,
			Result sixHours,
			Result twelveHours,
			Result twentyFourHours,
			Result sevenDays,
			Result twentyEightDays,
			Result monthToDate,
			Result yearToDate) {
	}

	/**
	 * Describes a result.
	 * @param value the value
	 * @param inches the inches
	 * @param millimeters the millimeters
	 * @param display the display
	 * @param unit the requested rainfall unit
	 * @param status the status
	 * @param completeness the completeness
	 * @param observationCutoff the observation cutoff
	 * @param warnings the warnings
	 * @author Gunnar Hillert
	 */
	public record Result(
			@Nullable BigDecimal value,
			@Nullable BigDecimal inches,
			@Nullable BigDecimal millimeters,
			String display,
			String unit,
			RainfallResultStatus status,
			BigDecimal completeness,
			@Nullable Instant observationCutoff,
			List<String> warnings) {

		/**
		 * Creates a new {@code Result}.
		 * @param value the value
		 * @param inches the inches
		 * @param millimeters the millimeters
		 * @param display the display
		 * @param unit the requested rainfall unit
		 * @param status the status
		 * @param completeness the completeness
		 * @param observationCutoff the observation cutoff
		 * @param warnings the warnings
		 */
		public Result {
			warnings = List.copyOf(warnings);
		}
	}

	/**
	 * Describes a daily rainfall.
	 * @param date the date
	 * @param value the value
	 * @param inches the inches
	 * @param millimeters the millimeters
	 * @param display the display
	 * @param status the status
	 * @author Gunnar Hillert
	 */
	public record DailyRainfall(
			LocalDate date,
			@Nullable BigDecimal value,
			@Nullable BigDecimal inches,
			@Nullable BigDecimal millimeters,
			String display,
			RainfallResultStatus status) {
	}

	/**
	 * Contains the increment, daily, and cumulative rainfall chart series.
	 * @param increments the increments
	 * @param daily the daily
	 * @param cumulative the cumulative
	 * @author Gunnar Hillert
	 */
	public record Charts(List<ChartPoint> increments, List<ChartPoint> daily,
			List<ChartPoint> cumulative) {

		/**
		 * Creates a new {@code Charts}.
		 * @param increments the increments
		 * @param daily the daily
		 * @param cumulative the cumulative
		 */
		public Charts {
			increments = List.copyOf(increments);
			daily = List.copyOf(daily);
			cumulative = List.copyOf(cumulative);
		}
	}

	/**
	 * Describes a chart point.
	 * @param timestamp the timestamp
	 * @param value the value
	 * @param inches the inches
	 * @param millimeters the millimeters
	 * @param quality the quality
	 * @author Gunnar Hillert
	 */
	public record ChartPoint(
			String timestamp,
			@Nullable BigDecimal value,
			@Nullable BigDecimal inches,
			@Nullable BigDecimal millimeters,
			@Nullable String quality) {
	}

	/**
	 * Describes a source.
	 * @param provider the provider
	 * @param fetchedAt the fetched at
	 * @param cacheAgeSeconds the cache age seconds
	 * @param staleCacheUsed the stale cache used
	 * @param nativeCadence the native cadence
	 * @param nativeUnit the native unit
	 * @param sourceResolution the source resolution
	 * @author Gunnar Hillert
	 */
	public record Source(String provider, Instant fetchedAt, long cacheAgeSeconds,
			boolean staleCacheUsed, String nativeCadence, String nativeUnit,
			BigDecimal sourceResolution) {
	}

	/**
	 * Describes a source discrepancy.
	 * @param date the date
	 * @param calculatedInches the calculated inches
	 * @param validationInches the validation inches
	 * @param differenceInches the difference inches
	 * @param validationSource the validation source
	 * @author Gunnar Hillert
	 */
	public record SourceDiscrepancy(LocalDate date, BigDecimal calculatedInches,
			BigDecimal validationInches, BigDecimal differenceInches, String validationSource) {
	}

}
