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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.pembana.raingauge.cocorahs.CoCoRaHSClient.ReportYear;
import com.pembana.raingauge.cocorahs.DailyReportService.RefreshCheck;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationType;
import com.pembana.raingauge.station.client.ProviderException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifies CoCoRaHS report parsing, arithmetic, and cache behavior.
 * @author OpenAI Codex
 */
class DailyReportTests {

	private static final LocalDate FIRST = LocalDate.of(2024, 1, 1);

	private static final LocalDate LAST = LocalDate.of(2024, 12, 31);

	private static final LocalDate TODAY = LocalDate.of(2024, 2, 13);

	private static final Instant NOW = Instant.parse("2024-02-13T18:00:00Z");

	private static final String STATION_ID = "HI-HI-1";

	private final Station station = new Station(StationType.COCORAHS_NETWORK, STATION_ID, "Naalehu");

	@Test
	void preservesCorrectedReadingsTraceZeroAndMissingCoverage() throws IOException {
		DailyReportService service = serviceWithFixture();
		DailyReportsResponse response = service.load(this.station, LocalDate.of(2024, 1, 29),
				LocalDate.of(2024, 2, 5), RainfallUnit.IMPERIAL, false);

		assertThat(response.display()).isEqualTo("0.35 in + trace");
		assertThat(response.days()).extracting(DailyReportsResponse.Day::display)
				.containsExactly("0.10 in", "Missing", "Missing", "Missing", "0 in", "Trace", "Missing", "Missing");
		assertThat(response.days().getFirst().report().observedAt().toString()).isEqualTo("2024-01-29T06:19");
		assertThat(response.coverage()).isEqualTo(new DailyReportsResponse.Coverage(8, 3, 6));
		assertThat(response.spans()).singleElement().satisfies((span) -> assertThat(span.included()).isTrue());
		assertThat(response.status()).isEqualTo(RainfallResultStatus.PARTIAL);
	}

	@Test
	void excludesBoundarySpansAndAmbiguousOverlaps() throws IOException {
		DailyReportService service = serviceWithFixture();
		DailyReportsResponse boundary = service.load(this.station, LocalDate.of(2024, 2, 1),
				LocalDate.of(2024, 2, 3), RainfallUnit.IMPERIAL, false);
		assertThat(boundary.total()).isZero();
		assertThat(boundary.trace()).isTrue();
		assertThat(boundary.spans().getFirst().included()).isFalse();

		DailyReportsResponse overlap = service.load(this.station, LocalDate.of(2024, 2, 8),
				TODAY, RainfallUnit.IMPERIAL, false);
		assertThat(overlap.status()).isEqualTo(RainfallResultStatus.CONFLICTING);
		assertThat(overlap.total()).isNull();
		assertThat(overlap.days().getLast().display()).isEqualTo("Pending");
		assertThat(overlap.spans()).allMatch((span) -> !span.included());
	}

	@Test
	void marksFallbackStaleAndExpiresItAfterSevenDays() throws IOException {
		CoCoRaHSClient client = fixtureClient();
		AtomicLong ticker = new AtomicLong();
		DailyReportService service = new DailyReportService(client, Clock.fixed(NOW, ZoneOffset.UTC), ticker::get, Runnable::run);
		LocalDate date = LocalDate.of(2024, 1, 29);
		assertThat(service.load(this.station, date, date, RainfallUnit.IMPERIAL, false).status())
				.isEqualTo(RainfallResultStatus.COMPLETE);

		willThrow(new ProviderException("Fixture outage")).given(client).fetch(STATION_ID, 2024, TODAY);
		ticker.set(Duration.ofHours(2).toNanos());
		DailyReportsResponse fallback = service.load(this.station, date, date, RainfallUnit.IMPERIAL, false);
		assertThat(fallback.status()).isEqualTo(RainfallResultStatus.STALE);
		assertThat(fallback.display()).isEqualTo("0.10 in");
		assertThat(fallback.source().staleCache()).isTrue();

		ticker.set(Duration.ofDays(7).plusSeconds(1).toNanos());
		assertThatThrownBy(() -> service.load(this.station, date, date, RainfallUnit.IMPERIAL, false))
				.isInstanceOf(ProviderException.class);
	}

	@Test
	void concurrentDateAndSeriesChecksShareOneFetch() throws Exception {
		CoCoRaHSClient client = mock(CoCoRaHSClient.class);
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch finish = new CountDownLatch(1);
		given(client.fetch(STATION_ID, 2024, TODAY)).willAnswer((invocation) -> {
			started.countDown();
			assertThat(finish.await(5, TimeUnit.SECONDS)).isTrue();
			return new ReportYear(List.of(), NOW);
		});
		DailyReportService service = new DailyReportService(client, Clock.fixed(NOW, ZoneOffset.UTC));
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Future<List<RefreshCheck>> day = executor.submit(() -> service.refreshRange(this.station,
					TODAY.minusDays(27), TODAY, TODAY));
			try {
				assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
				CountDownLatch otherChecks = new CountDownLatch(8);
				List<Future<List<RefreshCheck>>> series = new ArrayList<>();
				for (int index = 0; index < 8; index++) {
					series.add(executor.submit(() -> {
						otherChecks.countDown();
						return service.refreshYears(this.station, List.of(2024));
					}));
				}
				assertThat(otherChecks.await(5, TimeUnit.SECONDS)).isTrue();
				finish.countDown();
				List<RefreshCheck> first = day.get(5, TimeUnit.SECONDS);
				for (Future<List<RefreshCheck>> check : series) {
					assertThat(check.get(5, TimeUnit.SECONDS)).isEqualTo(first);
				}
				verify(client).fetch(STATION_ID, 2024, TODAY);
			}
			finally {
				finish.countDown();
			}
		}
	}

	@Test
	void failedCheckPreservesReportsAndCooldownUntilNextAttempt() throws IOException {
		CoCoRaHSClient client = fixtureClient();
		AtomicLong ticker = new AtomicLong();
		DailyReportService service = new DailyReportService(client, Clock.fixed(NOW, ZoneOffset.UTC),
				ticker::get, Runnable::run);
		LocalDate date = LocalDate.of(2024, 1, 29);
		assertThat(service.load(this.station, date, date, RainfallUnit.IMPERIAL, false).display())
				.isEqualTo("0.10 in");

		ticker.set(Duration.ofMinutes(5).toNanos());
		willThrow(new ProviderException("Fixture outage")).given(client).fetch(STATION_ID, 2024, TODAY);
		List<RefreshCheck> failed = service.refreshRange(this.station, date, TODAY, date);
		assertThat(failed).containsExactly(new RefreshCheck(2024, NOW, false));

		willReturn(new ReportYear(List.of(), NOW)).given(client).fetch(STATION_ID, 2024, TODAY);
		ticker.set(Duration.ofMinutes(10).minusNanos(1).toNanos());
		assertThat(service.refreshRange(this.station, date, TODAY, null)).isEqualTo(failed);
		assertThat(service.load(this.station, date, date, RainfallUnit.IMPERIAL, false).display())
				.isEqualTo("0.10 in");
		verify(client, times(2)).fetch(STATION_ID, 2024, TODAY);

		ticker.set(Duration.ofMinutes(10).toNanos());
		assertThat(service.refreshYears(this.station, List.of(2024))).containsExactly(
				new RefreshCheck(2024, NOW, true));
		assertThat(service.load(this.station, date, date, RainfallUnit.IMPERIAL, false).days())
				.extracting(DailyReportsResponse.Day::display).containsExactly("Missing");
		verify(client, times(3)).fetch(STATION_ID, 2024, TODAY);
	}

	@Test
	void refreshRangeTargetsOneYearForARowAndEveryYearForTheSeries() {
		CoCoRaHSClient client = mock(CoCoRaHSClient.class);
		given(client.fetch(STATION_ID, 2023, LocalDate.of(2023, 12, 31))).willReturn(new ReportYear(List.of(), NOW));
		given(client.fetch(STATION_ID, 2024, TODAY)).willReturn(new ReportYear(List.of(), NOW));
		DailyReportService service = new DailyReportService(client, Clock.fixed(NOW, ZoneOffset.UTC));
		LocalDate from = LocalDate.of(2023, 12, 31);

		assertThat(service.refreshRange(this.station, from, TODAY, TODAY)).extracting(RefreshCheck::year)
				.containsExactly(2024);
		assertThat(service.refreshRange(this.station, from, TODAY, null)).extracting(RefreshCheck::year)
				.containsExactly(2023, 2024);
		verify(client).fetch(STATION_ID, 2023, from);
		verify(client).fetch(STATION_ID, 2024, TODAY);
	}

	@Test
	void rejectsXmlWithExternalEntities() {
		byte[] payload = """
				<!DOCTYPE Cocorahs [<!ENTITY external SYSTEM "file:///report.xml">]>
				<Cocorahs><DailyPrecipReports>&external;</DailyPrecipReports></Cocorahs>
				""".getBytes(StandardCharsets.UTF_8);
		assertThatThrownBy(() -> CoCoRaHSClient.parse(new ByteArrayInputStream(payload),
				STATION_ID, FIRST, LAST, false)).isInstanceOf(ProviderException.class);
	}

	private DailyReportService serviceWithFixture() throws IOException {
		return new DailyReportService(fixtureClient(), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private CoCoRaHSClient fixtureClient() throws IOException {
		CoCoRaHSClient client = mock(CoCoRaHSClient.class);
		List<DailyReport> reports = new ArrayList<>(parse("daily", false));
		reports.addAll(parse("multi-day", true));
		given(client.fetch(STATION_ID, 2024, TODAY)).willReturn(new ReportYear(reports, NOW));
		return client;
	}

	private List<DailyReport> parse(String name, boolean multiDay) throws IOException {
		try (InputStream input = new ClassPathResource("fixtures/cocorahs/" + name + ".xml").getInputStream()) {
			return CoCoRaHSClient.parse(input, STATION_ID, FIRST, LAST, multiDay);
		}
	}

}
