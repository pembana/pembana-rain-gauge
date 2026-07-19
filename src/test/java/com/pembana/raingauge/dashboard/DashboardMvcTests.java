package com.pembana.raingauge.dashboard;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.PrecipitationObservation;
import com.pembana.raingauge.observation.client.HadsObservationClient;
import com.pembana.raingauge.observation.client.IemDailySummaryClient;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationOverride;
import com.pembana.raingauge.station.StationRepository;
import com.pembana.raingauge.station.client.CatalogStation;
import com.pembana.raingauge.station.client.IemStationCatalogClient;
import com.pembana.raingauge.station.client.StationCatalogResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
@SpringBootTest(properties = "hawaii.rainfall.catalog.startup-enabled=false")
class DashboardMvcTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StationRepository repository;

	@MockitoBean
	private HadsObservationClient observationClient;

	@MockitoBean
	private IemDailySummaryClient dailySummaryClient;

	@MockitoBean
	private IemStationCatalogClient catalogClient;

	@BeforeEach
	void setUp() {
		this.repository.deleteAll();
		Station waiaha = station("WIHH1", "Kailua-Kona 3SE - Waiaha", true);
		waiaha.applyOverride(new StationOverride("HI82", "Waiaha", "Hawaiʻi", "North Kona",
				true, true, null, "PCIRG", "fixture"));
		this.repository.saveAndFlush(waiaha);
		when(this.dailySummaryClient.fetch(anyString(), anyString(), any())).thenReturn(Map.of());
		when(this.observationClient.fetch(anyList(), anyString(), anyString(), any(), any()))
				.thenAnswer((invocation) -> {
					Instant from = invocation.getArgument(3);
					Instant to = invocation.getArgument(4);
					List<PrecipitationObservation> observations = List.of(
							PrecipitationObservation.valid("WIHH1", from, "PCIRG",
									new BigDecimal("10.00"), 0),
							PrecipitationObservation.valid("WIHH1",
									to.minus(Duration.ofMinutes(15)),
									"PCIRG", new BigDecimal("10.05"), 1));
					return new ObservationBatch(observations, List.of(), Instant.now(),
							Duration.ZERO, false, false, "fixture", 0);
				});
		when(this.catalogClient.fetchCompleteCatalog("HI_DCP")).thenReturn(new StationCatalogResult(
				List.of(new CatalogStation("HI_DCP", "WIHH1",
						"Kailua-Kona 3SE - Waiaha", null, null, null, null, null, true,
						"HI", "US", "Pacific/Honolulu", "fixture")),
				List.of(), 0));
	}

	@Test
	void dashboardRendersCompleteServerPageAndSelector() throws Exception {
		this.mockMvc.perform(get("/").queryParam("station", "WIHH1")
				.queryParam("period", "28d").queryParam("unit", "imperial"))
				.andExpect(status().isOk())
				.andExpect(view().name("dashboard"))
				.andExpect(content().string(containsString("Pembana")))
				.andExpect(content().string(containsString("Waiaha (HI82)")))
				.andExpect(content().string(containsString("method=\"get\"")))
				.andExpect(content().string(containsString("Provisional data")))
				.andExpect(content().string(containsString("daily-rainfall-table")));
	}

	@Test
	void dashboardApiReturnsConsistentAggregateShape() throws Exception {
		this.mockMvc.perform(get("/api/stations/WIHH1/dashboard")
				.queryParam("period", "28d").queryParam("unit", "imperial"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.station.stationId").value("WIHH1"))
				.andExpect(jsonPath("$.station.displayName").value("Waiaha"))
				.andExpect(jsonPath("$.summary.twentyEightDays.status").exists())
				.andExpect(jsonPath("$.observationCutoff").exists())
				.andExpect(jsonPath("$.charts.cumulative").isArray());
	}

	@Test
	void invalidPeriodUsesRfc9457ProblemDetail() throws Exception {
		this.mockMvc.perform(get("/api/stations/WIHH1/dashboard").queryParam("period", "forever"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.title").value("Invalid request"))
				.andExpect(jsonPath("$.type").value("https://pembana.com/problems/invalid-request"));
	}

	@Test
	void disabledStationIsRejectedByPublicApi() throws Exception {
		Station disabled = station("OFFH1", "Disabled station", false);
		this.repository.saveAndFlush(disabled);

		this.mockMvc.perform(get("/api/stations/OFFH1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.title").value("Station not found"));
	}

	@Test
	void emptyCatalogRendersExplicitWarning() throws Exception {
		this.repository.deleteAll();

		this.mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Station catalog unavailable")));
	}

	@Test
	void jteEscapesProviderControlledStationName() throws Exception {
		this.repository.saveAndFlush(station("SAFEH1", "<script>alert('x')</script>", true));

		this.mockMvc.perform(get("/stations"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("&lt;script&gt;alert")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						containsString("<script>alert('x')</script>"))));
	}

	@Test
	void administratorRefreshRequiresAuthenticationAndCsrf() throws Exception {
		this.mockMvc.perform(post("/admin/station-catalog/refresh").with(csrf()))
				.andExpect(status().isUnauthorized());
		this.mockMvc.perform(post("/admin/station-catalog/refresh")
				.with(httpBasic("admin", "change-me")))
				.andExpect(status().isForbidden());
		this.mockMvc.perform(post("/admin/station-catalog/refresh")
				.with(httpBasic("admin", "change-me")).with(csrf()))
				.andExpect(status().isOk());
	}

	private Station station(String stationId, String sourceName, boolean enabled) {
		Station station = new Station("HI_DCP", stationId, sourceName);
		station.updateSourceMetadata(sourceName, null, null, null, true, null, null, "HI", "US",
				"Pacific/Honolulu", "fixture", Instant.now());
		if (!enabled) {
			station.applyOverride(new StationOverride(null, null, null, null, false, false,
					"disabled for test", null, null));
		}
		return station;
	}

}
