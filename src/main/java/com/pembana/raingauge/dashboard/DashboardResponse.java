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

	public DashboardResponse {
		dailyRainfall = List.copyOf(dailyRainfall);
		discrepancies = List.copyOf(discrepancies);
		warnings = List.copyOf(warnings);
	}

	public record Selection(String period, String unit, ZonedDateTime from, ZonedDateTime to) {
	}

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

		public Result {
			warnings = List.copyOf(warnings);
		}
	}

	public record DailyRainfall(
			LocalDate date,
			@Nullable BigDecimal value,
			@Nullable BigDecimal inches,
			@Nullable BigDecimal millimeters,
			String display,
			RainfallResultStatus status) {
	}

	public record Charts(List<ChartPoint> increments, List<ChartPoint> daily,
			List<ChartPoint> cumulative) {

		public Charts {
			increments = List.copyOf(increments);
			daily = List.copyOf(daily);
			cumulative = List.copyOf(cumulative);
		}
	}

	public record ChartPoint(
			String timestamp,
			@Nullable BigDecimal value,
			@Nullable BigDecimal inches,
			@Nullable BigDecimal millimeters,
			@Nullable String quality) {
	}

	public record Source(String provider, Instant fetchedAt, long cacheAgeSeconds,
			boolean staleCacheUsed, String nativeCadence, String nativeUnit,
			BigDecimal sourceResolution) {
	}

	public record SourceDiscrepancy(LocalDate date, BigDecimal calculatedInches,
			BigDecimal validationInches, BigDecimal differenceInches, String validationSource) {
	}

}
