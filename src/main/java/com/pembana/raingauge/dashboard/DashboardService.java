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
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.pembana.raingauge.observation.client.IemDailySummaryClient;
import com.pembana.raingauge.rainfall.RainfallAmount;
import com.pembana.raingauge.rainfall.RainfallIncrement;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallService;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationService;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Provides dashboard operations.
 * @author Gunnar Hillert
 */
@Service
public class DashboardService {

	private static final ZoneId HAWAII = ZoneId.of("Pacific/Honolulu");

	private static final Set<RainfallWindow> SUMMARY_WINDOWS = EnumSet.of(
			RainfallWindow.ONE_HOUR, RainfallWindow.THREE_HOURS, RainfallWindow.SIX_HOURS,
			RainfallWindow.TWELVE_HOURS, RainfallWindow.TWENTY_FOUR_HOURS,
			RainfallWindow.SEVEN_DAYS, RainfallWindow.TWENTY_EIGHT_DAYS,
			RainfallWindow.MONTH_TO_DATE, RainfallWindow.YEAR_TO_DATE);

	private final RainfallService rainfallService;

	private final IemDailySummaryClient dailySummaryClient;

	private final StationService stationService;

	/**
	 * Creates a new {@code DashboardService}.
	 * @param rainfallService the rainfall service
	 * @param dailySummaryClient the daily summary client
	 * @param stationService the station service
	 */
	public DashboardService(RainfallService rainfallService,
			IemDailySummaryClient dailySummaryClient, StationService stationService) {
		this.rainfallService = rainfallService;
		this.dailySummaryClient = dailySummaryClient;
		this.stationService = stationService;
	}

	/**
	 * Builds the page metadata from the configured site URL.
	 * @param station the station to process
	 * @param selectedWindow the selected window
	 * @param unit the requested rainfall unit
	 * @return the resulting build
	 */
	@Cacheable(cacheNames = "dashboard",
			key = "#station.stationId + ':' + #selectedWindow.token() + ':' + #unit.token()")
	public DashboardResponse build(Station station, RainfallWindow selectedWindow,
			RainfallUnit unit) {
		Set<RainfallWindow> windows = EnumSet.copyOf(SUMMARY_WINDOWS);
		windows.add(selectedWindow);
		Map<RainfallWindow, RainfallResult> results =
				this.rainfallService.calculateWindows(station, windows, unit);
		RainfallResult selected = results.get(selectedWindow);
		if (selected == null) {
			throw new IllegalStateException("The selected rainfall result was not calculated");
		}
		if (selected.observationCutoff() != null) {
			this.stationService.recordLatestObservation(station, selected.observationCutoff());
		}
		List<DashboardResponse.DailyRainfall> daily = daily(selected, unit);
		DashboardResponse.Charts charts = charts(selected, daily, unit);
		List<String> warnings = new ArrayList<>(selected.warnings());
		List<DashboardResponse.SourceDiscrepancy> discrepancies =
				validateDaily(station, selected, daily, warnings);
		long cacheAgeSeconds = Math.max(0,
				Duration.between(selected.fetchedAt(), selected.calculatedAt()).toSeconds());
		return new DashboardResponse(StationResponse.from(station),
				new DashboardResponse.Selection(selectedWindow.token(), unit.token(),
						selected.requestedStart().atZone(HAWAII),
						selected.requestedEnd().atZone(HAWAII)),
				selected.calculatedAt(), selected.observationCutoff(), summary(results),
				selected.quality(), daily, charts,
				new DashboardResponse.Source(selected.provider(), selected.fetchedAt(),
						cacheAgeSeconds,
						selected.quality().staleCacheUsed(), cadenceLabel(selected),
						selected.nativeUnit(),
						selected.sourceResolution()), discrepancies, warnings);
	}

	/**
	 * Builds dashboard summary values for standard rainfall windows.
	 * @param results the results
	 * @return the resulting summary
	 */
	private DashboardResponse.Summary summary(Map<RainfallWindow, RainfallResult> results) {
		return new DashboardResponse.Summary(
				view(results.get(RainfallWindow.ONE_HOUR)),
				view(results.get(RainfallWindow.THREE_HOURS)),
				view(results.get(RainfallWindow.SIX_HOURS)),
				view(results.get(RainfallWindow.TWELVE_HOURS)),
				view(results.get(RainfallWindow.TWENTY_FOUR_HOURS)),
				view(results.get(RainfallWindow.SEVEN_DAYS)),
				view(results.get(RainfallWindow.TWENTY_EIGHT_DAYS)),
				view(results.get(RainfallWindow.MONTH_TO_DATE)),
				view(results.get(RainfallWindow.YEAR_TO_DATE)));
	}

	/**
	 * Maps a rainfall calculation to its dashboard view model.
	 * @param result the result
	 * @return the resulting view
	 */
	private DashboardResponse.Result view(RainfallResult result) {
		RainfallAmount amount = result.amount();
		return new DashboardResponse.Result(amount == null ? null : amount.value(result.unit()),
				amount == null ? null : amount.inches(), amount == null ? null : amount.millimeters(),
				result.displayValue(), result.unit().symbol(), result.status(),
				result.quality().completenessPercentage(), result.observationCutoff(),
				result.warnings());
	}

	/**
	 * Returns daily rainfall data for a station.
	 * @param selected the selected
	 * @param unit the requested rainfall unit
	 * @return the resulting daily
	 */
	private List<DashboardResponse.DailyRainfall> daily(RainfallResult selected,
			RainfallUnit unit) {
		Map<LocalDate, BigDecimal> totals = new LinkedHashMap<>();
		for (RainfallIncrement increment : selected.increments()) {
			LocalDate date = increment.at().atZone(HAWAII).toLocalDate();
			totals.merge(date, increment.inches(), BigDecimal::add);
		}
		LocalDate firstDate = selected.requestedStart().atZone(HAWAII).toLocalDate();
		LocalDate lastDate = selected.requestedEnd().minusNanos(1).atZone(HAWAII).toLocalDate();
		List<DashboardResponse.DailyRainfall> daily = new ArrayList<>();
		for (LocalDate date = firstDate; !date.isAfter(lastDate); date = date.plusDays(1)) {
			BigDecimal inches = totals.get(date);
			if (inches == null) {
				daily.add(new DashboardResponse.DailyRainfall(date, null, null, null,
						"No observations", RainfallResultStatus.UNAVAILABLE));
			} else {
				RainfallAmount amount = new RainfallAmount(inches);
				daily.add(new DashboardResponse.DailyRainfall(date, amount.value(unit), amount.inches(),
						amount.millimeters(), amount.display(unit), selected.status()));
			}
		}
		return List.copyOf(daily);
	}

	/**
	 * Builds rainfall chart series from calculated results.
	 * @param selected the selected
	 * @param daily the daily
	 * @param unit the requested rainfall unit
	 * @return the resulting charts
	 */
	private DashboardResponse.Charts charts(RainfallResult selected,
			List<DashboardResponse.DailyRainfall> daily, RainfallUnit unit) {
		List<DashboardResponse.ChartPoint> increments = new ArrayList<>();
		List<DashboardResponse.ChartPoint> cumulative = new ArrayList<>();
		for (RainfallIncrement increment : selected.increments()) {
			RainfallAmount amount = new RainfallAmount(increment.inches());
			RainfallAmount cumulativeAmount = new RainfallAmount(increment.cumulativeInches());
			increments.add(new DashboardResponse.ChartPoint(increment.at().toString(), amount.value(unit),
					amount.inches(), amount.millimeters(), increment.qualityFlag()));
			cumulative.add(new DashboardResponse.ChartPoint(increment.at().toString(),
					cumulativeAmount.value(unit), cumulativeAmount.inches(),
					cumulativeAmount.millimeters(), increment.qualityFlag()));
		}
		List<DashboardResponse.ChartPoint> dailyPoints = daily.stream()
				.map((item) -> new DashboardResponse.ChartPoint(item.date().toString(), item.value(),
						item.inches(), item.millimeters(),
						item.status() == RainfallResultStatus.UNAVAILABLE ? "missing" : null))
				.toList();
		return new DashboardResponse.Charts(increments, dailyPoints, cumulative);
	}

	/**
	 * Validates daily.
	 * @param station the station to process
	 * @param selected the selected
	 * @param daily the daily
	 * @param warnings the warnings
	 * @return the resulting validate daily
	 */
	private List<DashboardResponse.SourceDiscrepancy> validateDaily(Station station,
			RainfallResult selected, List<DashboardResponse.DailyRainfall> daily,
			List<String> warnings) {
		Map<YearMonth, Map<LocalDate, BigDecimal>> summaries = new LinkedHashMap<>();
		List<DashboardResponse.SourceDiscrepancy> discrepancies = new ArrayList<>();
		try {
			for (DashboardResponse.DailyRainfall item : daily) {
				if (!isCompleteLocalDay(selected, item) || item.inches() == null) {
					continue;
				}
				YearMonth month = YearMonth.from(item.date());
				Map<LocalDate, BigDecimal> validation = summaries.computeIfAbsent(month,
						(ignored) -> this.dailySummaryClient.fetch(station.getNetwork(),
								station.getStationId(), month));
				BigDecimal validationValue = validation.get(item.date());
				if (validationValue != null && item.inches().compareTo(validationValue) != 0) {
					BigDecimal difference = item.inches().subtract(validationValue);
					discrepancies.add(new DashboardResponse.SourceDiscrepancy(item.date(),
							item.inches(), validationValue, difference, "IEM daily API"));
					warnings.add("Daily validation differs on " + item.date()
							+ ": raw observations "
							+ item.inches().toPlainString() + " in; IEM daily API "
							+ validationValue.toPlainString() + " in");
				}
			}
		} catch (ProviderException ex) {
			warnings.add("Daily validation source was unavailable: " + ex.getMessage());
		}
		return List.copyOf(discrepancies);
	}

	/**
	 * Returns whether complete local day.
	 * @param selected the selected
	 * @param item the item
	 * @return {@code true} if complete local day; otherwise {@code false}
	 */
	private boolean isCompleteLocalDay(RainfallResult selected,
			DashboardResponse.DailyRainfall item) {
		return item.status() == RainfallResultStatus.COMPLETE
				&& !selected.requestedStart().isAfter(item.date().atStartOfDay(HAWAII).toInstant())
				&& !selected.requestedEnd().isBefore(item.date().plusDays(1)
						.atStartOfDay(HAWAII).toInstant());
	}

	/**
	 * Formats a human-readable observation cadence.
	 * @param result the result
	 * @return the resulting cadence label
	 */
	private String cadenceLabel(RainfallResult result) {
		long seconds = result.quality().expectedSamples() == 0 ? 0
				: Duration.between(result.requestedStart(), result.requestedEnd()).toSeconds()
						/ result.quality().expectedSamples();
		return seconds % 60 == 0 ? (seconds / 60) + " minutes" : seconds + " seconds";
	}

}
