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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.dashboard.YearComparisonResponse.DailyPoint;
import com.pembana.raingauge.dashboard.YearComparisonResponse.MonthResult;
import com.pembana.raingauge.dashboard.YearComparisonResponse.YearResult;
import com.pembana.raingauge.rainfall.RainfallAmount;
import com.pembana.raingauge.rainfall.RainfallIncrement;
import com.pembana.raingauge.rainfall.RainfallMethod;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallService;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.RainfallCapability;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Compares rainfall across Hawaiʻi calendar years using native station observations.
 * @author OpenAI Codex
 */
@Service
public class YearComparisonService {

	private static final int FIRST_ARCHIVE_YEAR = 2010;

	private static final int MAX_YEARS = 8;

	private final RainfallService rainfallService;

	private final Clock clock;

	private final Cache<YearQuery, YearResult> historical;

	private final Cache<YearQuery, YearResult> recent;

	YearComparisonService(RainfallService rainfallService, RainfallProperties properties,
			Clock clock) {
		this.rainfallService = rainfallService;
		this.clock = clock;
		this.historical = cache(properties.getCache().getHistoricalObservations());
		this.recent = cache(properties.getCache().getObservations());
	}

	List<Integer> availableYears(Station station) {
		LocalDate begin = station.getArchiveBegin();
		LocalDate end = station.getArchiveEnd();
		int latest = LocalDate.now(this.clock.withZone(RainfallWindow.HAWAII)).getYear();
		if (end != null) {
			latest = Math.min(latest, end.getYear());
		}
		int first = (begin != null) ? begin.getYear() : FIRST_ARCHIVE_YEAR;
		return IntStream.rangeClosed(first, latest).mapToObj((year) -> year).toList();
	}

	YearComparisonResponse compare(Station station, List<Integer> requested, String mode,
			RainfallUnit unit) {
		if (!mode.equals("calendar") && !mode.equals("ytd")) {
			throw new IllegalArgumentException("Choose calendar years or matching year to date");
		}
		if (requested.size() > MAX_YEARS) {
			throw new IllegalArgumentException("Choose up to eight years");
		}
		List<Integer> available = availableYears(station);
		List<Integer> years = requested.isEmpty()
				? available.stream().skip(Math.max(0, available.size() - 3)).toList()
				: requested.stream().distinct().sorted().toList();
		if (!available.containsAll(years)) {
			throw new IllegalArgumentException("Choose years within the station's archive");
		}

		// A stable cutoff lets repeated requests reuse current data for up to five minutes.
		Instant now = this.clock.instant();
		long seconds = now.getEpochSecond();
		Instant cutoff = Instant.ofEpochSecond(seconds - Math.floorMod(seconds, 300));
		LocalDateTime localCutoff = LocalDateTime.ofInstant(cutoff, RainfallWindow.HAWAII);
		List<YearResult> results = new ArrayList<>();
		for (int year : years) {
			LocalDateTime from = LocalDate.of(year, 1, 1).atStartOfDay();
			LocalDateTime to = (mode.equals("ytd") || year == localCutoff.getYear())
					? localCutoff.withYear(year) : from.plusYears(1);
			results.add(load(station, from, to, unit, year == localCutoff.getYear()));
		}
		return new YearComparisonResponse(StationResponse.from(station), unit.token(), mode,
				List.copyOf(results));
	}

	private YearResult load(Station station, LocalDateTime from, LocalDateTime to,
			RainfallUnit unit, boolean current) {
		if (!from.isBefore(to)) {
			return unavailable(from, to, unit, "The period has just started");
		}
		YearQuery query = new YearQuery(station.getNetwork(), station.getStationId(),
				station.getPrecipitationKey(), station.getRainfallCapability(), from, to, unit);
		Cache<YearQuery, YearResult> cache = current ? this.recent : this.historical;
		try {
			YearResult result = cache.get(query, (key) -> aggregate(from, to,
					this.rainfallService.calculate(station, from.atZone(RainfallWindow.HAWAII).toInstant(),
							to.atZone(RainfallWindow.HAWAII).toInstant(), unit)));
			if (result.staleCache()) {
				cache.asMap().remove(query, result);
			}
			return result;
		}
		catch (ProviderException ex) {
			return unavailable(from, to, unit, "The observation provider could not load this year");
		}
	}

	private YearResult aggregate(LocalDateTime from, LocalDateTime to, RainfallResult result) {
		Map<LocalDate, BigDecimal> daily = new HashMap<>();
		Map<Integer, BigDecimal> monthly = new HashMap<>();
		for (RainfallIncrement increment : result.increments()) {
			Instant at = (result.method() == RainfallMethod.INTERVAL)
					? increment.at().minusNanos(1) : increment.at();
			LocalDate date = at.atZone(RainfallWindow.HAWAII).toLocalDate();
			daily.merge(date, increment.inches(), BigDecimal::add);
			monthly.merge(date.getMonthValue(), increment.inches(), BigDecimal::add);
		}

		List<DailyPoint> cumulative = new ArrayList<>();
		BigDecimal running = BigDecimal.ZERO;
		for (LocalDate day = from.toLocalDate(); day.atStartOfDay().isBefore(to);
				day = day.plusDays(1)) {
			BigDecimal inches = daily.get(day);
			if (inches == null) {
				cumulative.add(new DailyPoint(day, null));
			}
			else {
				running = running.add(inches);
				cumulative.add(new DailyPoint(day, new RainfallAmount(running).value(result.unit())));
			}
		}

		RainfallAmount amount = daily.isEmpty() ? null : result.amount();
		RainfallResultStatus status = result.status();
		List<String> warnings = new ArrayList<>(result.warnings());
		if (amount == null) {
			status = RainfallResultStatus.UNAVAILABLE;
		}
		else if (result.quality().receivedSamples() < result.quality().expectedSamples()
				|| (status == RainfallResultStatus.STALE && !result.quality().staleCacheUsed())) {
			if (status != RainfallResultStatus.CONFLICTING) {
				status = RainfallResultStatus.PARTIAL;
			}
			warnings.add("Observations do not cover the full requested period");
		}
		return new YearResult(from.getYear(), from, to, !to.equals(from.plusYears(1)),
				(amount != null) ? amount.value(result.unit()) : null,
				(amount != null) ? amount.display(result.unit()) : "Unavailable", status,
				result.quality().completenessPercentage(), result.observationCutoff(),
				result.provider(), result.fetchedAt(), result.quality().staleCacheUsed(),
				List.copyOf(warnings), months(monthly, result.unit()), List.copyOf(cumulative));
	}

	private YearResult unavailable(LocalDateTime from, LocalDateTime to, RainfallUnit unit,
			String warning) {
		return new YearResult(from.getYear(), from, to, !to.equals(from.plusYears(1)), null,
				"Unavailable", RainfallResultStatus.UNAVAILABLE, null, null, null, null, false,
				List.of(warning), months(Map.of(), unit), List.of());
	}

	private List<MonthResult> months(Map<Integer, BigDecimal> inches, RainfallUnit unit) {
		return IntStream.rangeClosed(1, 12).mapToObj((month) -> {
			BigDecimal value = inches.get(month);
			if (value == null) {
				return new MonthResult(month, null, "—");
			}
			RainfallAmount amount = new RainfallAmount(value);
			return new MonthResult(month, amount.value(unit), amount.display(unit));
		}).toList();
	}

	private static Cache<YearQuery, YearResult> cache(Duration ttl) {
		return Caffeine.newBuilder().maximumSize(512).expireAfterWrite(ttl).recordStats().build();
	}

	private record YearQuery(String network, String stationId,
			@Nullable String precipitationKey,
			RainfallCapability capability, LocalDateTime from, LocalDateTime to, RainfallUnit unit) {

	}

}
