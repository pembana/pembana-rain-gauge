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

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.pembana.raingauge.station.client.IemStationVariableClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests rainfall capability discovery.
 * @author Gunnar Hillert
 */
class RainfallCapabilityServiceTests {

	/**
	 * Verifies cumulative precipitation remains preferred when both semantics exist.
	 */
	@Test
	void prefersCumulativePrecipitation() {
		RainfallCapabilityService.CapabilityDiscovery discovery =
				discover(Set.of("PPHRGZZ", "PCIRGZZ"));

		assertThat(discovery.capability())
				.isEqualTo(RainfallCapability.SUPPORTED_ACCUMULATOR);
		assertThat(discovery.precipitationKey()).isEqualTo("PCIRG");
	}

	/**
	 * Verifies the shortest supported fixed interval is selected.
	 */
	@Test
	void selectsShortestSupportedFixedInterval() {
		RainfallCapabilityService.CapabilityDiscovery discovery =
				discover(Set.of("PPDRZ", "PPHRGZZ", "PPCRZ"));

		assertThat(discovery.capability())
				.isEqualTo(RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION);
		assertThat(discovery.precipitationKey()).isEqualTo("PPCRZ");
	}

	/**
	 * Verifies variable-duration precipitation is retained as unsupported metadata.
	 */
	@Test
	void rejectsVariableDurationInterval() {
		RainfallCapabilityService.CapabilityDiscovery discovery =
				discover(Set.of("PPVRGZZ"));

		assertThat(discovery.capability()).isEqualTo(RainfallCapability.UNSUPPORTED);
		assertThat(discovery.precipitationKey()).isEqualTo("PPVRG");
	}

	/**
	 * Discovers capability from the supplied provider variables.
	 * @param variables the provider variables
	 * @return the discovered capability
	 */
	private RainfallCapabilityService.CapabilityDiscovery discover(Set<String> variables) {
		IemStationVariableClient client = mock(IemStationVariableClient.class);
		given(client.fetchRecentVariables("TEST1")).willReturn(variables);
		return new RainfallCapabilityService(client)
				.discover(new Station("HI_DCP", "TEST1", "Test station"));
	}

}
