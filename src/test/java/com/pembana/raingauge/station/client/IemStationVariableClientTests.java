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

package com.pembana.raingauge.station.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests IEM station variable client.
 * @author Gunnar Hillert
 */
class IemStationVariableClientTests {

	/**
	 * Verifies that parses actual variable discovery shape.
	 * @throws IOException if an I/O operation fails
	 */
	@Test
	void parsesActualVariableDiscoveryShape() throws IOException {
		ProviderRestClientFactory factory = mock(ProviderRestClientFactory.class);
		given(factory.create(org.mockito.ArgumentMatchers.anyString())).willReturn(mock(RestClient.class));
		IemStationVariableClient client = new IemStationVariableClient(factory, new RainfallProperties());
		String fixture = new ClassPathResource("fixtures/iem-wihh1-vars.json")
				.getContentAsString(StandardCharsets.UTF_8);

		assertThat(client.parse(fixture)).containsExactlyInAnyOrder("PPHRGZZ", "PCIRGZZ");
	}

}
