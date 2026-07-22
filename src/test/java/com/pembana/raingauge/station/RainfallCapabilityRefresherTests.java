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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pembana.raingauge.config.RainfallProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests rainfall capability refresher.
 * @author Gunnar Hillert
 */
class RainfallCapabilityRefresherTests {

	private StationRepository repository;

	private RainfallCapabilityService capabilityService;

	private RainfallCapabilityRefresher refresher;

	/**
	 * Creates isolated refresher collaborators before each test.
	 */
	@BeforeEach
	void setUp() {
		this.repository = mock(StationRepository.class);
		this.capabilityService = mock(RainfallCapabilityService.class);
		RainfallProperties properties = new RainfallProperties();
		properties.getCatalog().setCapabilityRefreshConcurrency(2);
		this.refresher = new RainfallCapabilityRefresher(this.repository,
				this.capabilityService, properties);
	}

	/**
	 * Verifies that refresh discovers and persists unknown capabilities.
	 */
	@Test
	void refreshDiscoversAndPersistsUnknownCapabilities() {
		Station accumulator = new Station("HI_DCP", "RAINH1", "Accumulator");
		Station interval = new Station("HI_DCP", "HLRH1", "Interval");
		given(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc())
				.willReturn(List.of(accumulator, interval));
		given(this.capabilityService.discover(accumulator)).willReturn(
				new RainfallCapabilityService.CapabilityDiscovery(
						RainfallCapability.SUPPORTED_ACCUMULATOR, "PCIRG"));
		given(this.capabilityService.discover(interval)).willReturn(
				new RainfallCapabilityService.CapabilityDiscovery(
						RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION, "PPHRZ"));

		RainfallCapabilityRefresher.CapabilityRefreshSummary summary = this.refresher.refresh();

		assertThat(summary.checked()).isEqualTo(2);
		assertThat(summary.supportedAccumulators()).isEqualTo(1);
		assertThat(summary.intervalPrecipitation()).isEqualTo(1);
		assertThat(accumulator.getPrecipitationKey()).isEqualTo("PCIRG");
		assertThat(interval.getRainfallCapability())
				.isEqualTo(RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION);
		then(this.repository).should().save(accumulator);
		then(this.repository).should().save(interval);
	}

	/**
	 * Verifies that refresh skips stations whose capability is already known.
	 */
	@Test
	void refreshSkipsStationsWhoseCapabilityIsAlreadyKnown() {
		Station known = new Station("HI_DCP", "WIHH1", "Known");
		known.updateCapability(RainfallCapability.SUPPORTED_ACCUMULATOR, "PCIRG");
		given(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc())
				.willReturn(List.of(known));

		RainfallCapabilityRefresher.CapabilityRefreshSummary summary = this.refresher.refresh();

		assertThat(summary.checked()).isZero();
		then(this.capabilityService).should(never()).discover(known);
		then(this.repository).should(never()).save(known);
	}

}
