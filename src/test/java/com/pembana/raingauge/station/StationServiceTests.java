package com.pembana.raingauge.station;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.station.client.CatalogStation;
import com.pembana.raingauge.station.client.IemStationCatalogClient;
import com.pembana.raingauge.station.client.ProviderException;
import com.pembana.raingauge.station.client.StationCatalogResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StationServiceTests {

	private static final Instant NOW = Instant.parse("2026-07-19T08:00:00Z");

	private StationRepository repository;

	private IemStationCatalogClient catalogClient;

	private RainfallProperties properties;

	private StationService service;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		this.repository = mock(StationRepository.class);
		this.catalogClient = mock(IemStationCatalogClient.class);
		this.properties = new RainfallProperties();
		TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
		when(transactionTemplate.execute(any())).thenAnswer((invocation) -> {
			TransactionCallback<Object> callback = invocation.getArgument(0);
			return callback.doInTransaction(new SimpleTransactionStatus());
		});
		this.service = new StationService(this.repository, this.catalogClient, this.properties,
				transactionTemplate, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void emptyDatabaseTriggersCatalogRetrieval() {
		when(this.repository.count()).thenReturn(0L);
		when(this.repository.findAll()).thenReturn(List.of());
		when(this.catalogClient.fetchCompleteCatalog("HI_DCP")).thenReturn(result(station("WIHH1")));

		assertThat(this.service.initializeCatalogIfEmpty()).isTrue();
		verify(this.catalogClient).fetchCompleteCatalog("HI_DCP");
		verify(this.repository).saveAll(any());
	}

	@Test
	void populatedDatabaseSkipsBootstrapRetrieval() {
		when(this.repository.count()).thenReturn(1L);

		assertThat(this.service.initializeCatalogIfEmpty()).isFalse();
		verify(this.catalogClient, never()).fetchCompleteCatalog(any());
	}

	@Test
	void failedBootstrapPermitsStartupByDefault() {
		when(this.repository.count()).thenReturn(0L);
		when(this.catalogClient.fetchCompleteCatalog("HI_DCP"))
				.thenThrow(new ProviderException("provider down"));

		assertThat(this.service.initializeCatalogIfEmpty()).isFalse();
	}

	@Test
	void strictBootstrapFailsOnlyWhenDatabaseIsEmpty() {
		this.properties.getCatalog().setFailStartupWhenEmpty(true);
		when(this.repository.count()).thenReturn(0L);
		when(this.catalogClient.fetchCompleteCatalog("HI_DCP"))
				.thenThrow(new ProviderException("provider down"));

		assertThatThrownBy(this.service::initializeCatalogIfEmpty)
				.isInstanceOf(ProviderException.class)
				.hasMessageContaining("provider down");
	}

	@Test
	@SuppressWarnings("unchecked")
	void refreshAddsStationAndAppliesAllConfiguredDisplayOverrides() {
		this.properties.getStationOverrides().put("WIHH1", new StationOverride("HI82", "Waiaha",
				"Hawaiʻi", "North Kona", true, true, null, "PCIRG", "Featured station"));
		when(this.repository.findAll()).thenReturn(List.of());
		when(this.catalogClient.fetchCompleteCatalog("HI_DCP")).thenReturn(result(station("WIHH1")));

		StationService.CatalogRefreshSummary summary = this.service.refreshCatalog();

		ArgumentCaptor<Collection<Station>> saved = ArgumentCaptor.forClass(Collection.class);
		verify(this.repository).saveAll(saved.capture());
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

	@Test
	void refreshUpdatesExistingAndRetainsAbsentStationAsUnconfirmed() {
		Station existing = new Station("HI_DCP", "WIHH1", "Old source name");
		Station absent = new Station("HI_DCP", "OLDH1", "Absent from this response");
		when(this.repository.findAll()).thenReturn(List.of(existing, absent));
		when(this.catalogClient.fetchCompleteCatalog("HI_DCP")).thenReturn(result(station("WIHH1")));

		StationService.CatalogRefreshSummary summary = this.service.refreshCatalog();

		assertThat(summary.updated()).isEqualTo(1);
		assertThat(summary.unconfirmed()).isEqualTo(1);
		assertThat(existing.getSourceName()).isEqualTo("Kailua-Kona 3SE - Waiaha");
		assertThat(absent.isCatalogConfirmed()).isFalse();
	}

	private StationCatalogResult result(CatalogStation station) {
		return new StationCatalogResult(List.of(station), List.of(), 0);
	}

	private CatalogStation station(String stationId) {
		return new CatalogStation("HI_DCP", stationId, "Kailua-Kona 3SE - Waiaha",
				new BigDecimal("19.6333"), new BigDecimal("-155.9489"),
				new BigDecimal("470.335"), LocalDate.of(2012, 3, 20), null, true,
				"HI", "US", "Pacific/Honolulu", "fixture");
	}

}
