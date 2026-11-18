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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.pembana.raingauge.cocorahs.DailyReport;
import com.pembana.raingauge.cocorahs.DailyReportService;
import com.pembana.raingauge.cocorahs.DailyReportService.RefreshCheck;
import com.pembana.raingauge.cocorahs.DailyReportsResponse;
import com.pembana.raingauge.dashboard.YearComparisonResponse.DailyPoint;
import com.pembana.raingauge.dashboard.YearComparisonResponse.MonthResult;
import com.pembana.raingauge.dashboard.YearComparisonResponse.ReportDetails;
import com.pembana.raingauge.dashboard.YearComparisonResponse.YearResult;
import com.pembana.raingauge.rainfall.RainfallAmount;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Builds year comparisons using observation dates and preserves undivided multi-day amounts.
 * @author OpenAI Codex
 */
@Component
class DailyYearComparison {

	private final DailyReportService reports;

	DailyYearComparison(DailyReportService reports) {
		this.reports = reports;
	}

	List<RefreshCheck> refresh(Station station, List<Integer> years) {
		return this.reports.refreshYears(station, years);
	}

	YearResult load(Station station, int year, String mode, RainfallUnit unit) {
		LocalDate today = this.reports.today();
		LocalDate from = LocalDate.of(year, 1, 1);
		LocalDate until = from.plusYears(1);
		boolean matchingYtd = "ytd".equals(mode);
		boolean current = year == today.getYear();
		if (matchingYtd) {
			LocalDate yesterday = today.minusDays(1);
			until = (yesterday.getYear() == today.getYear()) ? yesterday.withYear(year).plusDays(1) : from;
		}
		else if (current) {
			until = today.plusDays(1);
		}
		boolean yearToDate = matchingYtd || current;
		if (until.equals(from)) {
			return unavailable(from, until, yearToDate, "The observation period has just started.");
		}
		try {
			DailyReportsResponse data = this.reports.load(station, from, until.minusDays(1), unit, false);
			return summarize(data, until, yearToDate, unit);
		}
		catch (ProviderException _) {
			return unavailable(from, until, yearToDate, "CoCoRaHS could not load reports for this year.");
		}
	}

	private YearResult summarize(DailyReportsResponse data, LocalDate until, boolean yearToDate,
			RainfallUnit unit) {
		Map<Integer, Amount> months = new HashMap<>();
		Map<Integer, Integer> expectedDates = new HashMap<>();
		Map<LocalDate, DailyReportsResponse.Span> spansByEnd = new HashMap<>();
		Amount unallocated = new Amount();
		for (DailyReportsResponse.Span span : data.spans()) {
			if (span.included()) {
				DailyReport report = span.report();
				spansByEnd.put(report.end(), span);
				if (YearMonth.from(report.start()).equals(YearMonth.from(report.end()))) {
					months.computeIfAbsent(report.end().getMonthValue(), (_) -> new Amount()).add(report);
				}
				else {
					unallocated.add(report);
				}
			}
		}
		Amount running = new Amount();
		List<DailyPoint> cumulative = new ArrayList<>();
		LocalDateTime cutoff = null;
		for (DailyReportsResponse.Day day : data.days()) {
			if (!"Pending".equals(day.display())) {
				expectedDates.merge(day.date().getMonthValue(), 1, Integer::sum);
			}
			DailyReport daily = day.report();
			DailyReportsResponse.Span span = spansByEnd.get(day.date());
			if (span != null) {
				DailyReport report = span.report();
				running.add(report);
				cutoff = report.observedAt();
				cumulative.add(new DailyPoint(day.date(), running.value(unit),
						"Multi-day report " + report.start() + " to " + report.end() + " · " + span.display()));
			}
			else if (daily != null && day.included()) {
				running.add(daily);
				cutoff = daily.observedAt();
				months.computeIfAbsent(day.date().getMonthValue(), (_) -> new Amount()).add(daily);
				cumulative.add(new DailyPoint(day.date(), running.value(unit), daily.trace() ? "Trace" : null));
			}
			else {
				cumulative.add(new DailyPoint(day.date(), null));
			}
		}
		DailyReportsResponse.Coverage coverage = data.coverage();
		BigDecimal completeness = (coverage.expectedDays() > 0)
				? BigDecimal.valueOf(100L * coverage.coveredDays())
						.divide(BigDecimal.valueOf(coverage.expectedDays()), 1, RoundingMode.HALF_UP) : null;
		Instant observationCutoff = (cutoff != null) ? cutoff.atZone(RainfallWindow.HAWAII).toInstant() : null;
		return new YearResult(data.from().getYear(), data.from().atStartOfDay(), until.atStartOfDay(), yearToDate,
				data.total(), data.display(), data.status(), completeness, observationCutoff,
				data.source().provider(), data.source().fetchedAt(), data.source().staleCache(),
				data.warnings(), months(months, expectedDates, unit), List.copyOf(cumulative),
				new ReportDetails(coverage, data.trace(), unallocated.value(unit), unallocated.trace,
						unallocated.display(unit), data.spans()));
	}

	private YearResult unavailable(LocalDate from, LocalDate until, boolean yearToDate, String warning) {
		return new YearResult(from.getYear(), from.atStartOfDay(), until.atStartOfDay(), yearToDate,
				null, "Unavailable", RainfallResultStatus.UNAVAILABLE, null, null, DailyReportService.PROVIDER,
				null, false, List.of(warning), months(Map.of(), Map.of(), RainfallUnit.METRIC), List.of(), null);
	}

	private List<MonthResult> months(Map<Integer, Amount> amounts, Map<Integer, Integer> expectedDates,
			RainfallUnit unit) {
		return IntStream.rangeClosed(1, 12).mapToObj((month) -> {
			Amount amount = amounts.get(month);
			if (amount == null) {
				return new MonthResult(month, null, "—");
			}
			int expected = expectedDates.getOrDefault(month, 0);
			String display = amount.display(unit) + ((amount.days < expected) ? " · Partial" : "");
			return new MonthResult(month, amount.value(unit), display);
		}).toList();
	}

	private static final class Amount {

		private @Nullable BigDecimal inches;

		private boolean trace;

		private int days;

		void add(DailyReport report) {
			BigDecimal amount = report.inches();
			if (amount != null) {
				BigDecimal previous = this.inches;
				this.inches = (previous != null) ? previous.add(amount) : amount;
			}
			this.trace |= report.trace();
			this.days += (int) ChronoUnit.DAYS.between(report.start(), report.end()) + 1;
		}

		@Nullable BigDecimal value(RainfallUnit unit) {
			BigDecimal amount = this.inches;
			return (amount != null) ? new RainfallAmount(amount).value(unit) : null;
		}

		String display(RainfallUnit unit) {
			BigDecimal amount = this.inches;
			if (amount == null) {
				return this.trace ? "Trace" : "—";
			}
			return new RainfallAmount(amount).display(unit) + (this.trace ? " + trace" : "");
		}

	}

}
