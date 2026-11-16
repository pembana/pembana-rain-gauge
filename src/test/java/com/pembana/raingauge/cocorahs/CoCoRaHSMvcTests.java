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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.pembana.raingauge.station.RainfallCapability;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationRepository;
import com.pembana.raingauge.station.StationType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises volunteer routes against representative CoCoRaHS XML exports.
 * @author OpenAI Codex
 */
@AutoConfigureMockMvc
@SpringBootTest(properties = {
		"hawaii.rainfall.catalog.startup-enabled=false",
		"hawaii.rainfall.administrator.username=admin",
		"hawaii.rainfall.administrator.password={noop}change-me"
})
class CoCoRaHSMvcTests {

	private static final Instant NOW = Instant.parse("2028-03-01T18:00:00Z");

	private static final HttpServer EXPORTS = startExports();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private StationRepository repository;

	@DynamicPropertySource
	static void configure(DynamicPropertyRegistry registry) {
		registry.add("hawaii.rainfall.providers.cocorahs-base-url",
				() -> "http://127.0.0.1:" + EXPORTS.getAddress().getPort());
	}

	@AfterAll
	static void stopExports() {
		EXPORTS.stop(0);
	}

	@BeforeEach
	void saveStation() {
		this.repository.deleteAll();
		Station station = new Station(StationType.COCORAHS_NETWORK, "HI-HI-1", "Naalehu");
		station.updateCapability(RainfallCapability.DAILY_REPORTS, null);
		station.updateSourceMetadata(new Station.SourceMetadata("Naalehu", null, null, null,
				false, LocalDate.of(2023, 1, 1), LocalDate.of(2024, 12, 31),
				"HI", "US", "Pacific/Honolulu", null), NOW);
		this.repository.saveAndFlush(station);
	}

	@Test
	void browsesHistoricalStationAndShowsReportsWithoutJavaScript() throws Exception {
		this.mvc.perform(get("/stations").param("network", StationType.COCORAHS_NETWORK))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("CoCoRaHS · Daily reports")))
				.andExpect(content().string(containsString("/stations/HI-HI-1")));
		this.mvc.perform(get("/api/stations").param("network", StationType.COCORAHS_NETWORK))
				.andExpect(jsonPath("$[0].stationId").value("HI-HI-1"))
				.andExpect(jsonPath("$[0].type").value("DAILY_REPORTS"));
		this.mvc.perform(get("/stations/HI-HI-1").param("from", "2024-01-29")
				.param("to", "2024-02-04").param("unit", "imperial"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("0.35 in + trace")))
				.andExpect(content().string(containsString("06:19")))
				.andExpect(content().string(containsString("2024-01-30 to 2024-02-01")))
				.andExpect(content().string(containsString("Multi-day accumulations")))
				.andExpect(content().string(containsString("Check for update on 2024-02-04")))
				.andExpect(content().string(containsString("Refresh displayed series")))
				.andExpect(content().string(containsString("name=\"_csrf\"")));
		this.mvc.perform(get("/api/stations/HI-HI-1/daily-reports").param("from", "2024-01-29")
				.param("to", "2024-02-04"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodBasis").value("observation-date"))
				.andExpect(jsonPath("$.coverage.coveredDays").value(6))
				.andExpect(jsonPath("$.total").value(8.89))
				.andExpect(jsonPath("$.latestReport.observedAt").value("2024-02-11T07:00:00"));
		this.mvc.perform(get("/api/stations/HI-HI-1/dashboard")).andExpect(status().isUnprocessableContent());
		this.mvc.perform(get("/compare")).andExpect(content().string(not(containsString("HI-HI-1"))));
	}

	@Test
	void comparesYearsAndKeepsCrossMonthAmountsUnallocated() throws Exception {
		this.mvc.perform(get("/api/stations/HI-HI-1/years").param("year", "2023", "2024")
				.param("mode", "ytd").param("unit", "imperial"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periodBasis").value("observation-date"))
				.andExpect(jsonPath("$.years[0].to").value("2023-03-01T00:00:00"))
				.andExpect(jsonPath("$.years[0].reportDetails.coverage.expectedDays").value(59))
				.andExpect(jsonPath("$.years[1].to").value("2024-03-01T00:00:00"))
				.andExpect(jsonPath("$.years[1].reportDetails.coverage.expectedDays").value(60))
				.andExpect(jsonPath("$.years[1].value").value(0.35))
				.andExpect(jsonPath("$.years[1].months[0].value").value(0.10))
				.andExpect(jsonPath("$.years[1].months[0].display", containsString("Partial")))
				.andExpect(jsonPath("$.years[1].reportDetails.unallocatedValue").value(0.25))
				.andExpect(jsonPath("$.years[1].cumulative[31].annotation",
						containsString("Multi-day report")));
		this.mvc.perform(get("/compare/years").param("station", "HI-HI-1").param("year", "2024"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Unallocated across months")))
				.andExpect(content().string(containsString("Refresh displayed series")))
				.andExpect(content().string(containsString("2024-01-30 to 2024-02-01")));
	}

	@Test
	void refreshFormsPreserveTheirSelectionAndShowLastCheck() throws Exception {
		String stationPath = "/stations/HI-HI-1";
		String dateQuery = "?from=2024-01-29&to=2024-02-04&unit=imperial";
		this.mvc.perform(post(stationPath + "/refresh-reports")
				.param("from", "2024-01-29").param("to", "2024-02-04"))
				.andExpect(status().isForbidden());
		var daily = this.mvc.perform(post(stationPath + "/refresh-reports").with(csrf())
				.param("from", "2024-01-29").param("to", "2024-02-04")
				.param("unit", "imperial").param("date", "2024-02-04"))
				.andExpect(redirectedUrl(stationPath + dateQuery)).andReturn();
		this.mvc.perform(get(stationPath + dateQuery).flashAttrs(daily.getFlashMap()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Checked Mar 1, 2028 at 8:00 AM HST")))
				.andExpect(content().string(containsString("Report for 2024-02-04 · <strong>Missing</strong>")))
				.andExpect(content().string(containsString("0.35 in + trace")));

		String years = "/compare/years?station=HI-HI-1&year=2023&year=2024&mode=ytd&unit=imperial";
		var annual = this.mvc.perform(post("/compare/years/refresh-reports").with(csrf())
				.param("station", "HI-HI-1").param("year", "2023", "2024")
				.param("mode", "ytd").param("unit", "imperial"))
				.andExpect(redirectedUrl(years)).andReturn();
		this.mvc.perform(get(years).flashAttrs(annual.getFlashMap()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("five-minute cooldown")))
				.andExpect(content().string(containsString("0.35 in + trace")));
	}

	@Test
	void rejectsAnIndividualRefreshOutsideTheDisplayedRange() throws Exception {
		this.mvc.perform(post("/stations/HI-HI-1/refresh-reports").with(csrf())
				.param("from", "2024-01-29").param("to", "2024-02-04").param("date", "2023-01-01"))
				.andExpect(status().isBadRequest());
	}

	private static HttpServer startExports() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/cocorahs/export/exportreports.aspx", (exchange) -> {
				try (exchange) {
					String query = exchange.getRequestURI().getQuery();
					boolean multiDay = query.contains("ReportType=MultiDay");
					String type = multiDay ? "MultiDay" : "Daily";
					byte[] body;
					if (query.contains("Station=HI-HI-1") && query.contains("StartDate=2024-01-01")) {
						String fixture = multiDay ? "multi-day" : "daily";
						try (InputStream input = new ClassPathResource("fixtures/cocorahs/" + fixture + ".xml")
								.getInputStream()) {
							body = input.readAllBytes();
						}
					}
					else {
						body = ("<Cocorahs><" + type + "PrecipReports/></Cocorahs>")
								.getBytes(StandardCharsets.UTF_8);
					}
					exchange.getResponseHeaders().add("Content-Type", "application/xml");
					exchange.sendResponseHeaders(200, body.length);
					exchange.getResponseBody().write(body);
				}
			});
			server.start();
			return server;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedTime {

		@Bean
		@Primary
		Clock fixtureClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

	}

}
