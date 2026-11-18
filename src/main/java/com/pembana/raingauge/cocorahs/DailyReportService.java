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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pembana.raingauge.cocorahs.CoCoRaHSClient.ReportYear;
import com.pembana.raingauge.rainfall.RainfallAmount;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationType;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Loads CoCoRaHS exports and totals usable reports within observation-date boundaries.
 * @author OpenAI Codex
 */
@Service
public class DailyReportService {

	/** Basis for ranges expressed in the observers’ local dates. */
	public static final String PERIOD_BASIS = "observation-date";

	/** Observation provider name. */
	public static final String PROVIDER = "CoCoRaHS";

	/** Earliest year of the Hawaiʻi CoCoRaHS archive. */
	public static final int FIRST_HAWAII_YEAR = 2007;

	private static final DateTimeFormatter CHECK_TIME = DateTimeFormatter
			.ofPattern("MMM d, yyyy 'at' h:mm a 'HST'", Locale.ENGLISH)
			.withZone(RainfallWindow.HAWAII);

	private static final Duration REFRESH_COOLDOWN = Duration.ofMinutes(5);

	private static final Comparator<DailyReport> REVISION_ORDER = Comparator
			.comparing(DailyReport::revisedAt)
			.thenComparing(DailyReport::observedAt)
			.thenComparing(DailyReport::start)
			.thenComparing((report) -> String.valueOf(report.inches()))
			.thenComparing(DailyReport::trace);

	private final Cache<YearKey, ReportYear> recent;

	private final Cache<YearKey, ReportYear> historical;

	private final Cache<YearKey, ReportYear> fallback;

	private final AsyncCache<YearKey, RefreshAttempt> refreshes;

	private final CoCoRaHSClient client;

	private final Clock clock;

	private final Executor fetchExecutor;

	@Autowired
	DailyReportService(CoCoRaHSClient client, Clock clock) {
		this(client, clock, Ticker.systemTicker(), Thread::startVirtualThread);
	}

	DailyReportService(CoCoRaHSClient client, Clock clock, Ticker ticker, Executor fetchExecutor) {
		this.client = client;
		this.clock = clock;
		this.fetchExecutor = fetchExecutor;
		this.recent = cache(Duration.ofHours(1), ticker);
		this.historical = cache(Duration.ofDays(1), ticker);
		this.fallback = cache(Duration.ofDays(7), ticker);
		// Valid station/year keys are bounded by the catalog. Keep every attempt until its cooldown expires.
		this.refreshes = Caffeine.newBuilder().expireAfterWrite(REFRESH_COOLDOWN).ticker(ticker).buildAsync();
	}

	private static Cache<YearKey, ReportYear> cache(Duration ttl, Ticker ticker) {
		return Caffeine.newBuilder().maximumSize(512).expireAfterWrite(ttl).ticker(ticker).build();
	}

	public LocalDate today() {
		return LocalDate.now(this.clock.withZone(RainfallWindow.HAWAII));
	}

	public String display(@Nullable DailyReport report, RainfallUnit unit) {
		if (report == null) {
			return "Missing";
		}
		BigDecimal inches = report.inches();
		if (inches != null) {
			return (inches.signum() == 0) ? "0 " + unit.symbol() : new RainfallAmount(inches).display(unit);
		}
		return report.trace() ? "Trace" : "Missing";
	}

	public DailyReportsResponse load(Station station, LocalDate from, LocalDate to, RainfallUnit unit,
			boolean findLatest) {
		LocalDate today = today();
		validateRange(from, to, today);
		List<DailyReport> reports = new ArrayList<>();
		Instant fetchedAt = null;
		Map<Integer, CachedYear> loadedYears = new HashMap<>();
		boolean stale = false;
		for (int year : yearsFor(station, from, to)) {
			CachedYear loaded = loadYear(station, year, today);
			loadedYears.put(year, loaded);
			reports.addAll(loaded.data().reports());
			Instant yearFetchedAt = loaded.data().fetchedAt();
			if (fetchedAt == null || yearFetchedAt.isBefore(fetchedAt)) {
				fetchedAt = yearFetchedAt;
			}
			stale |= loaded.stale();
		}
		Latest latest = findLatest ? latest(station, today, loadedYears) : new Latest(null, false, false);
		return aggregate(station, from, to, today, unit, reports, latest, fetchedAt, stale);
	}

	/**
	 * Checks one date's year or all years intersecting the displayed range.
	 * @param station the volunteer station
	 * @param from the first displayed date
	 * @param to the last displayed date
	 * @param date the requested row, or null to refresh the range
	 * @return the last attempt for each requested year
	 */
	public List<RefreshCheck> refreshRange(Station station, LocalDate from, LocalDate to,
			@Nullable LocalDate date) {
		validateRange(from, to, today());
		if (date != null && (date.isBefore(from) || date.isAfter(to))) {
			throw new IllegalArgumentException("Choose a date within the displayed reports");
		}
		List<Integer> years = (date != null) ? yearsFor(station, date, date) : yearsFor(station, from, to);
		return refreshYears(station, years);
	}

	/**
	 * Checks the selected station years, sharing requests and outcomes during the cooldown.
	 * @param station the volunteer station
	 * @param years validated archive years from the current view
	 * @return the last attempt for each requested year
	 */
	public List<RefreshCheck> refreshYears(Station station, List<Integer> years) {
		if (station.getType() != StationType.DAILY_REPORTS) {
			throw new IllegalArgumentException("Report refresh is available for CoCoRaHS stations");
		}
		LocalDate today = today();
		return years.stream().distinct().map((year) -> refreshYear(station, year, today).check()).toList();
	}

	private static void validateRange(LocalDate from, LocalDate to, LocalDate today) {
		if (from.isAfter(to) || to.isAfter(today) || from.getYear() < FIRST_HAWAII_YEAR
				|| ChronoUnit.DAYS.between(from, to) >= 366) {
			throw new IllegalArgumentException("Choose an observation-date range of 1–366 days from 2007 through today");
		}
	}

	private List<Integer> yearsFor(Station station, LocalDate from, LocalDate to) {
		LocalDate archiveBegin = station.getArchiveBegin();
		LocalDate archiveEnd = station.getArchiveEnd();
		int first = (archiveBegin != null) ? Math.max(archiveBegin.getYear(), from.getYear()) : from.getYear();
		int last = (archiveEnd != null) ? Math.min(archiveEnd.getYear(), to.getYear()) : to.getYear();
		return IntStream.rangeClosed(first, last).boxed().toList();
	}

	private Latest latest(Station station, LocalDate today, Map<Integer, CachedYear> loadedYears) {
		List<Integer> years = yearsFor(station, LocalDate.of(FIRST_HAWAII_YEAR, 1, 1), today).reversed();
		for (int year : years) {
			try {
				CachedYear loaded = loadedYears.computeIfAbsent(year, (_) -> loadYear(station, year, today));
				DailyReport latest = corrected(loaded.data().reports()).stream()
						.max(Comparator.comparing(DailyReport::observedAt)).orElse(null);
				if (latest != null || loaded.stale()) {
					return new Latest(latest, loaded.stale(), false);
				}
			}
			catch (ProviderException _) {
				return new Latest(null, false, true);
			}
		}
		return new Latest(null, false, false);
	}

	private CachedYear loadYear(Station station, int year, LocalDate today) {
		YearKey key = new YearKey(station.getStationId(), year);
		ReportYear existing = reportsCache(year, today).getIfPresent(key);
		if (existing != null) {
			return new CachedYear(existing, false);
		}
		RefreshAttempt attempt = refreshYear(station, year, today);
		ReportYear loaded = attempt.data();
		if (loaded != null) {
			return new CachedYear(loaded, false);
		}
		ReportYear cached = this.fallback.getIfPresent(key);
		if (cached != null) {
			return new CachedYear(cached, true);
		}
		throw new ProviderException("Unable to refresh CoCoRaHS reports");
	}

	private RefreshAttempt refreshYear(Station station, int year, LocalDate today) {
		YearKey key = new YearKey(station.getStationId(), year);
		return this.refreshes.get(key, (requested, executor) -> CompletableFuture.supplyAsync(
				() -> fetchYear(requested, today), this.fetchExecutor)).join();
	}

	private RefreshAttempt fetchYear(YearKey key, LocalDate today) {
		LocalDate through = (key.year() == today.getYear()) ? today : LocalDate.of(key.year(), 12, 31);
		try {
			ReportYear loaded = this.client.fetch(key.station(), key.year(), through);
			reportsCache(key.year(), today).put(key, loaded);
			this.fallback.put(key, loaded);
			return new RefreshAttempt(loaded, new RefreshCheck(key.year(), loaded.fetchedAt(), true));
		}
		catch (ProviderException _) {
			// Store the failure as a result so Caffeine shares it and holds the cooldown.
			return new RefreshAttempt(null, new RefreshCheck(key.year(), this.clock.instant(), false));
		}
	}

	private Cache<YearKey, ReportYear> reportsCache(int year, LocalDate today) {
		return (year == today.getYear()) ? this.recent : this.historical;
	}

	private static List<DailyReport> corrected(List<DailyReport> reports) {
		Map<ReportKey, DailyReport> byDate = new HashMap<>();
		for (DailyReport report : reports) {
			ReportKey key = new ReportKey(report.start(), report.end(), report.multiDay());
			byDate.merge(key, report, (left, right) -> (REVISION_ORDER.compare(left, right) >= 0) ? left : right);
		}
		return byDate.values().stream().sorted(Comparator.comparing(DailyReport::observedAt)
				.thenComparing(DailyReport::multiDay)).toList();
	}

	DailyReportsResponse aggregate(Station station, LocalDate from, LocalDate to, LocalDate today,
			RainfallUnit unit, List<DailyReport> reports, Latest latest, @Nullable Instant fetchedAt, boolean stale) {
		List<DailyReport> inRange = corrected(reports).stream()
				.filter((report) -> !report.end().isBefore(from) && !report.start().isAfter(to))
				.toList();
		Set<DailyReport> conflicted = findConflicts(inRange);
		Map<LocalDate, DailyReport> daily = new HashMap<>();
		List<DailyReportsResponse.Span> spans = new ArrayList<>();
		Set<LocalDate> covered = new HashSet<>();
		BigDecimal sum = null;
		boolean trace = false;
		for (DailyReport report : inRange) {
			boolean crossesBoundary = report.start().isBefore(from) || report.end().isAfter(to);
			boolean conflict = conflicted.contains(report);
			boolean included = report.usable() && !crossesBoundary && !conflict;
			if (report.multiDay()) {
				String reason = conflict ? "Overlapping reports" : crossesBoundary ? "Crosses selected dates"
						: report.usable() ? "Included in total" : "Missing amount";
				spans.add(new DailyReportsResponse.Span(report, value(report, unit), display(report, unit),
						included, reason));
			}
			else {
				daily.put(report.end(), report);
			}
			if (included) {
				LocalDate first = report.start();
				first.datesUntil(report.end().plusDays(1)).forEach(covered::add);
				BigDecimal inches = report.inches();
				if (inches != null) {
					sum = (sum != null) ? sum.add(inches) : inches;
				}
				trace |= report.trace();
			}
		}
		List<DailyReportsResponse.Day> days = new ArrayList<>();
		int dailyCount = 0;
		boolean pendingToday = to.equals(today) && !daily.containsKey(today) && !covered.contains(today)
				&& (station.getArchiveEnd() == null || !station.getArchiveEnd().isBefore(today));
		for (LocalDate date : from.datesUntil(to.plusDays(1)).toList()) {
			DailyReport report = daily.get(date);
			BigDecimal value = null;
			String label = (date.equals(today) && pendingToday) ? "Pending" : "Missing";
			boolean included = false;
			if (report != null && conflicted.contains(report)) {
				label = "Conflict · " + display(report, unit);
			}
			else if (report != null && report.usable()) {
				label = display(report, unit);
				value = value(report, unit);
				included = true;
				dailyCount++;
			}
			else if (covered.contains(date)) {
				label = "Multi-day report";
			}
			days.add(new DailyReportsResponse.Day(date, report, value, label, included));
		}
		int expected = days.size() - (pendingToday ? 1 : 0);
		RainfallResultStatus status = status(conflicted, covered, expected, stale);
		List<String> warnings = new ArrayList<>();
		if (!conflicted.isEmpty()) {
			warnings.add("Overlapping reports were excluded from totals.");
		}
		if (covered.size() < expected) {
			warnings.add("Reports cover only part of the selected period.");
		}
		if (stale) {
			warnings.add("CoCoRaHS could not refresh these reports. Previously fetched reports are shown.");
		}
		if (latest.stale() || latest.unavailable()) {
			warnings.add("The station’s latest report could not be verified.");
		}
		RainfallAmount total = (sum != null) ? new RainfallAmount(sum) : null;
		String display = (total != null) ? total.display(unit) + (trace ? " + trace" : "")
				: (trace ? "Trace" : "Unavailable");
		return new DailyReportsResponse(StationResponse.from(station), from, to, unit.token(), PERIOD_BASIS,
				List.copyOf(days), List.copyOf(spans), latest.report(), (total != null) ? total.value(unit) : null,
				display, trace, new DailyReportsResponse.Coverage(expected, dailyCount, covered.size()), status,
				new DailyReportsResponse.Source(PROVIDER, "IEM " + station.getNetwork(), fetchedAt, stale),
				List.copyOf(warnings));
	}

	private static RainfallResultStatus status(Set<DailyReport> conflicted, Set<LocalDate> covered,
			int expected, boolean stale) {
		if (!conflicted.isEmpty()) {
			return RainfallResultStatus.CONFLICTING;
		}
		if (covered.isEmpty()) {
			return RainfallResultStatus.UNAVAILABLE;
		}
		if (stale) {
			return RainfallResultStatus.STALE;
		}
		return (covered.size() < expected) ? RainfallResultStatus.PARTIAL : RainfallResultStatus.COMPLETE;
	}

	private static Set<DailyReport> findConflicts(List<DailyReport> reports) {
		Set<DailyReport> conflicts = new HashSet<>();
		List<DailyReport> usable = reports.stream().filter(DailyReport::usable)
				.sorted(Comparator.comparing(DailyReport::start)).toList();
		for (int i = 0; i < usable.size(); i++) {
			DailyReport left = usable.get(i);
			for (int j = i + 1; j < usable.size() && !usable.get(j).start().isAfter(left.end()); j++) {
				conflicts.add(left);
				conflicts.add(usable.get(j));
			}
		}
		return conflicts;
	}

	static @Nullable BigDecimal value(DailyReport report, RainfallUnit unit) {
		BigDecimal inches = report.inches();
		return (inches != null) ? new RainfallAmount(inches).value(unit) : null;
	}

	record Latest(@Nullable DailyReport report, boolean stale, boolean unavailable) {
	}

	/**
	 * Result of the most recent CoCoRaHS check for a station year.
	 * @param year the report year
	 * @param checkedAt when the provider attempt finished
	 * @param succeeded whether CoCoRaHS returned a usable response
	 */
	public record RefreshCheck(int year, Instant checkedAt, boolean succeeded) {

		public String localTime() {
			return CHECK_TIME.format(this.checkedAt);
		}

	}

	private record RefreshAttempt(@Nullable ReportYear data, RefreshCheck check) {
	}

	private record YearKey(String station, int year) {
	}

	private record ReportKey(LocalDate start, LocalDate end, boolean multiDay) {
	}

	private record CachedYear(ReportYear data, boolean stale) {
	}

}
