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

package com.pembana.raingauge.station;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.station.client.CatalogStation;
import com.pembana.raingauge.station.client.IemStationCatalogClient;
import com.pembana.raingauge.station.client.ProviderException;
import com.pembana.raingauge.station.client.StationCatalogResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests station service.
 * @author Gunnar Hillert
 */
class StationServiceTests {

	private static final Instant NOW = Instant.parse("2026-07-19T08:00:00Z");

	private StationRepository repository;

	private IemStationCatalogClient catalogClient;

	private RainfallProperties properties;

	private ApplicationEventPublisher eventPublisher;

	private StationService service;

	/**
	 * Creates isolated station-service collaborators before each test.
	 */
	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		this.repository = mock(StationRepository.class);
		this.catalogClient = mock(IemStationCatalogClient.class);
		this.eventPublisher = mock(ApplicationEventPublisher.class);
		this.properties = new RainfallProperties();
		TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
		given(transactionTemplate.execute(any())).willAnswer((invocation) -> {
			TransactionCallback<Object> callback = invocation.getArgument(0);
			return callback.doInTransaction(new SimpleTransactionStatus());
		});
		this.service = new StationService(this.repository, this.catalogClient, this.properties,
				transactionTemplate, this.eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	/**
	 * Verifies that empty database triggers catalog retrieval.
	 */
	@Test
	void emptyDatabaseTriggersCatalogRetrieval() {
		given(this.repository.count()).willReturn(0L);
		given(this.repository.findAll()).willReturn(List.of());
		given(this.catalogClient.fetchCompleteCatalog("HI_DCP")).willReturn(result(station("WIHH1")));

		assertThat(this.service.initializeCatalogIfEmpty()).isTrue();
		then(this.catalogClient).should().fetchCompleteCatalog("HI_DCP");
		then(this.repository).should().saveAll(any());
		then(this.eventPublisher).should().publishEvent(any(StationCatalogRefreshedEvent.class));
	}

	/**
	 * Verifies that populated database skips bootstrap retrieval.
	 */
	@Test
	void populatedDatabaseSkipsBootstrapRetrieval() {
		given(this.repository.count()).willReturn(1L);

		assertThat(this.service.initializeCatalogIfEmpty()).isFalse();
		then(this.catalogClient).should(never()).fetchCompleteCatalog(any());
	}

	/**
	 * Verifies that failed bootstrap permits startup by default.
	 */
	@Test
	void failedBootstrapPermitsStartupByDefault() {
		given(this.repository.count()).willReturn(0L);
		given(this.catalogClient.fetchCompleteCatalog("HI_DCP"))
				.willThrow(new ProviderException("provider down"));

		assertThat(this.service.initializeCatalogIfEmpty()).isFalse();
	}

	/**
	 * Verifies that strict bootstrap fails only when database is empty.
	 */
	@Test
	void strictBootstrapFailsOnlyWhenDatabaseIsEmpty() {
		this.properties.getCatalog().setFailStartupWhenEmpty(true);
		given(this.repository.count()).willReturn(0L);
		given(this.catalogClient.fetchCompleteCatalog("HI_DCP"))
				.willThrow(new ProviderException("provider down"));

		assertThatThrownBy(this.service::initializeCatalogIfEmpty)
				.isInstanceOf(ProviderException.class)
				.hasMessageContaining("provider down");
	}

	/**
	 * Verifies that refresh adds station and applies all configured display overrides.
	 */
	@Test
	@SuppressWarnings("unchecked")
	void refreshAddsStationAndAppliesAllConfiguredDisplayOverrides() {
		this.properties.getStationOverrides().put("WIHH1", new StationOverride("HI82", "Waiaha",
				"Hawaiʻi", "North Kona", true, true, null, "PCIRG", "Featured station"));
		given(this.repository.findAll()).willReturn(List.of());
		given(this.catalogClient.fetchCompleteCatalog("HI_DCP")).willReturn(result(station("WIHH1")));

		StationService.CatalogRefreshSummary summary = this.service.refreshCatalog();

		ArgumentCaptor<Collection<Station>> saved = ArgumentCaptor.forClass(Collection.class);
		then(this.repository).should().saveAll(saved.capture());
		Station station = saved.getValue().iterator().next();
		assertThat(summary.added()).isEqualTo(1);
		assertThat(station.getDisplayName()).isEqualTo("Waiaha");
		assertThat(station.getSourceName()).isEqualTo("Kailua-Kona 3SE - Waiaha");
		assertThat(station.getAlias()).isEqualTo("HI82");
		assertThat(station.getIsland()).isEqualTo("Hawaiʻi");
		assertThat(station.getRegion()).isEqualTo("North Kona");
		assertThat(station.isFeatured()).isTrue();
		assertThat(station.getPrecipitationKey()).isEqualTo("PCIRG");
	}

	/**
	 * Verifies that refresh updates existing and retains absent station as unconfirmed.
	 */
	@Test
	void refreshUpdatesExistingAndRetainsAbsentStationAsUnconfirmed() {
		Station existing = new Station("HI_DCP", "WIHH1", "Old source name");
		Station absent = new Station("HI_DCP", "OLDH1", "Absent from this response");
		given(this.repository.findAll()).willReturn(List.of(existing, absent));
		given(this.catalogClient.fetchCompleteCatalog("HI_DCP")).willReturn(result(station("WIHH1")));

		StationService.CatalogRefreshSummary summary = this.service.refreshCatalog();

		assertThat(summary.updated()).isEqualTo(1);
		assertThat(summary.unconfirmed()).isEqualTo(1);
		assertThat(existing.getSourceName()).isEqualTo("Kailua-Kona 3SE - Waiaha");
		assertThat(absent.isCatalogConfirmed()).isFalse();
	}

	/**
	 * Verifies that rainfall stations returns only repository confirmed accumulators.
	 */
	@Test
	void rainfallStationsReturnsOnlyRepositoryConfirmedAccumulators() {
		Station supported = new Station("HI_DCP", "WIHH1", "Supported");
		supported.updateCapability(RainfallCapability.SUPPORTED_ACCUMULATOR, "PCIRG");
		given(this.repository.findRainfallStations(RainfallCapability.SUPPORTED_ACCUMULATOR))
				.willReturn(List.of(supported));

		assertThat(this.service.findRainfallStations()).containsExactly(supported);
	}

	/**
	 * Creates a station-catalog result for a test scenario.
	 * @param station the station to process
	 * @return the resulting result
	 */
	private StationCatalogResult result(CatalogStation station) {
		return new StationCatalogResult(List.of(station), List.of(), 0);
	}

	/**
	 * Creates a station for a test scenario.
	 * @param stationId the provider station identifier
	 * @return the resulting station
	 */
	private CatalogStation station(String stationId) {
		return new CatalogStation("HI_DCP", stationId, "Kailua-Kona 3SE - Waiaha",
				new BigDecimal("19.6333"), new BigDecimal("-155.9489"),
				new BigDecimal("470.335"), LocalDate.of(2012, 3, 20), null, true,
				"HI", "US", "Pacific/Honolulu", "fixture");
	}

}
