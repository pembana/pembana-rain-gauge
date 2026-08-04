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
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.pembana.raingauge.observation.client.IemDailySummaryClient;
import com.pembana.raingauge.rainfall.RainfallAmount;
import com.pembana.raingauge.rainfall.RainfallIncrement;
import com.pembana.raingauge.rainfall.RainfallMethod;
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
		Instant observationCutoff = selected.observationCutoff();
		if (observationCutoff != null) {
			this.stationService.recordLatestObservation(station, observationCutoff);
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
		return new DashboardResponse.Result((amount != null) ? amount.value(result.unit()) : null,
				(amount != null) ? amount.inches() : null, (amount != null) ? amount.millimeters() : null,
				result.displayValue(), result.unit().symbol(), result.status(),
				result.quality().completenessPercentage(), result.observationCutoff(),
				qualityConditions(result), result.warnings());
	}

	/**
	 * Builds the individual checks that explain a rainfall quality status.
	 * @param result the rainfall calculation
	 * @return the quality conditions
	 */
	private List<DashboardResponse.QualityCondition> qualityConditions(RainfallResult result) {
		if (result.method() == RainfallMethod.INTERVAL) {
			return intervalQualityConditions(result);
		}
		String baselineWarning = warningStartingWith(result, "No valid accumulator baseline");
		String gapWarning = warningStartingWith(result, "Native observations contain a gap");
		String outlierWarning = warningStartingWith(result, "Unusually large positive increment");
		return List.of(
				condition("Starting baseline available", baselineWarning == null,
						baselineWarning, "A valid reading exists at or before the period start."),
				condition("Expected observation count",
						result.quality().completenessPercentage().compareTo(BigDecimal.valueOf(100)) >= 0,
						"Fewer valid observations were received than expected.",
						"The expected number of valid observations was received."),
				condition("No material observation gaps", gapWarning == null,
						gapWarning, "No gap exceeded twice the expected observation cadence."),
				condition("No unresolved accumulator resets",
						result.quality().unresolvedResetCount() == 0,
						result.quality().unresolvedResetCount() + " unresolved reset(s) were detected.",
						"All accumulator changes were resolved."),
				condition("No conflicting duplicate observations",
						result.quality().conflictingObservationCount() == 0,
						result.quality().conflictingObservationCount() + " conflicting duplicate(s) were detected.",
						"No conflicting duplicate observations were detected."),
				condition("No suspected rainfall outliers", outlierWarning == null,
						outlierWarning, "No unusually large positive increment was detected."));
	}

	/**
	 * Builds quality checks specific to fixed-duration interval precipitation.
	 * @param result the interval rainfall calculation
	 * @return the interval quality conditions
	 */
	private List<DashboardResponse.QualityCondition> intervalQualityConditions(
			RainfallResult result) {
		String boundaryWarning = warningStartingWith(result,
				"Requested range is not fully covered");
		if (boundaryWarning == null) {
			boundaryWarning = warningStartingWith(result,
					"An interval crossing the requested start");
		}
		String gapWarning = warningStartingWith(result,
				"Interval observations leave an uncovered gap");
		String overlapWarning = warningStartingWith(result,
				"Overlapping precipitation interval");
		String negativeWarning = warningStartingWith(result,
				"Negative interval precipitation");
		String outlierWarning = warningStartingWith(result,
				"Unusually large interval precipitation");
		return List.of(
				condition("Requested boundary covered by complete intervals",
						boundaryWarning == null, boundaryWarning,
						"No interval needed to be divided at the requested boundary."),
				condition("Expected interval count",
						result.quality().completenessPercentage()
								.compareTo(BigDecimal.valueOf(100)) >= 0,
						"Fewer complete intervals were received than expected.",
						"The expected number of complete intervals was received."),
				condition("No material interval gaps", gapWarning == null,
						gapWarning, "The selected intervals provide continuous coverage."),
				condition("No overlapping intervals", overlapWarning == null,
						overlapWarning, "No overlapping interval was detected."),
				condition("No invalid negative amounts", negativeWarning == null,
						negativeWarning, "No negative interval amount was detected."),
				condition("No conflicting duplicate observations",
						result.quality().conflictingObservationCount() == 0,
						result.quality().conflictingObservationCount()
								+ " conflicting duplicate(s) were detected.",
						"No conflicting duplicate observations were detected."),
				condition("No suspected rainfall outliers", outlierWarning == null,
						outlierWarning, "No unusually large interval amount was detected."));
	}

	/**
	 * Creates a displayable quality condition.
	 * @param label the condition label
	 * @param passed whether the condition passed
	 * @param failureDetail detail shown when the condition failed
	 * @param successDetail detail shown when the condition passed
	 * @return the quality condition
	 */
	private DashboardResponse.QualityCondition condition(String label, boolean passed,
			@Nullable String failureDetail, String successDetail) {
		String detail = successDetail;
		if (!passed) {
			detail = (failureDetail != null) ? failureDetail : "Condition failed.";
		}
		return new DashboardResponse.QualityCondition(label, passed, detail);
	}

	/**
	 * Finds the first calculation warning with the supplied prefix.
	 * @param result the rainfall calculation
	 * @param prefix the warning prefix
	 * @return the warning, or {@code null} when none matches
	 */
	private @Nullable String warningStartingWith(RainfallResult result, String prefix) {
		return result.warnings().stream()
				.filter((warning) -> warning.startsWith(prefix))
				.findFirst()
				.orElse(null);
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
			Instant attributionTime = (selected.method() == RainfallMethod.INTERVAL)
					? increment.at().minusNanos(1) : increment.at();
			LocalDate date = attributionTime.atZone(HAWAII).toLocalDate();
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
			}
			else {
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
						(item.status() == RainfallResultStatus.UNAVAILABLE) ? "missing" : null))
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
				BigDecimal inches = item.inches();
				if (!isCompleteLocalDay(selected, item) || inches == null) {
					continue;
				}
				YearMonth month = YearMonth.from(item.date());
				Map<LocalDate, BigDecimal> validation = summaries.computeIfAbsent(month,
						(ignored) -> this.dailySummaryClient.fetch(station.getNetwork(),
								station.getStationId(), month));
				BigDecimal validationValue = validation.get(item.date());
				if (validationValue != null && inches.compareTo(validationValue) != 0) {
					BigDecimal difference = inches.subtract(validationValue);
					discrepancies.add(new DashboardResponse.SourceDiscrepancy(item.date(),
							inches, validationValue, difference, "IEM daily API"));
					warnings.add("Daily validation differs on " + item.date()
							+ ": raw observations "
							+ inches.toPlainString() + " in; IEM daily API "
							+ validationValue.toPlainString() + " in");
				}
			}
		}
		catch (ProviderException ex) {
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
		long seconds = (result.quality().expectedSamples() == 0) ? 0
				: Duration.between(result.requestedStart(), result.requestedEnd()).toSeconds()
						/ result.quality().expectedSamples();
		return (seconds % 60 == 0) ? (seconds / 60) + " minutes" : seconds + " seconds";
	}

}
