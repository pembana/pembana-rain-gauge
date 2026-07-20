package com.pembana.raingauge.station;

import java.util.List;

import com.pembana.raingauge.config.RainfallProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RainfallCapabilityRefresherTests {

	private StationRepository repository;

	private RainfallCapabilityService capabilityService;

	private RainfallCapabilityRefresher refresher;

	@BeforeEach
	void setUp() {
		this.repository = mock(StationRepository.class);
		this.capabilityService = mock(RainfallCapabilityService.class);
		RainfallProperties properties = new RainfallProperties();
		properties.getCatalog().setCapabilityRefreshConcurrency(2);
		this.refresher = new RainfallCapabilityRefresher(this.repository,
				this.capabilityService, properties);
	}

	@Test
	void refreshDiscoversAndPersistsUnknownCapabilities() {
		Station accumulator = new Station("HI_DCP", "RAINH1", "Accumulator");
		Station interval = new Station("HI_DCP", "HLRH1", "Interval");
		when(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc())
				.thenReturn(List.of(accumulator, interval));
		when(this.capabilityService.discover(accumulator)).thenReturn(
				new RainfallCapabilityService.CapabilityDiscovery(
						RainfallCapability.SUPPORTED_ACCUMULATOR, "PCIRG"));
		when(this.capabilityService.discover(interval)).thenReturn(
				new RainfallCapabilityService.CapabilityDiscovery(
						RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION, "PPHRZ"));

		RainfallCapabilityRefresher.CapabilityRefreshSummary summary = this.refresher.refresh();

		assertThat(summary.checked()).isEqualTo(2);
		assertThat(summary.supportedAccumulators()).isEqualTo(1);
		assertThat(summary.intervalPrecipitation()).isEqualTo(1);
		assertThat(accumulator.getPrecipitationKey()).isEqualTo("PCIRG");
		assertThat(interval.getRainfallCapability())
				.isEqualTo(RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION);
		verify(this.repository).save(accumulator);
		verify(this.repository).save(interval);
	}

	@Test
	void refreshSkipsStationsWhoseCapabilityIsAlreadyKnown() {
		Station known = new Station("HI_DCP", "WIHH1", "Known");
		known.updateCapability(RainfallCapability.SUPPORTED_ACCUMULATOR, "PCIRG");
		when(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc())
				.thenReturn(List.of(known));

		RainfallCapabilityRefresher.CapabilityRefreshSummary summary = this.refresher.refresh();

		assertThat(summary.checked()).isZero();
		verify(this.capabilityService, never()).discover(known);
		verify(this.repository, never()).save(known);
	}

}
