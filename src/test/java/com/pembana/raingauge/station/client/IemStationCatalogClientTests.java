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
import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.support.ProviderStatusRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests IEM station catalog client.
 * @author Gunnar Hillert
 */
class IemStationCatalogClientTests {

	/**
	 * Verifies that parses representative GeoJSON and optional fields.
	 * @throws IOException if an I/O operation fails
	 */
	@Test
	void parsesRepresentativeGeoJsonAndOptionalFields() throws IOException {
		StationCatalogResult result = client().parse(fixture("iem-hi-dcp-catalog.json"), "HI_DCP");

		assertThat(result.stations()).hasSize(2);
		CatalogStation waiaha = result.stations().getFirst();
		assertThat(waiaha.stationId()).isEqualTo("WIHH1");
		assertThat(waiaha.sourceName()).isEqualTo("Kailua-Kona 3SE - Waiaha");
		assertThat(waiaha.latitude()).isEqualByComparingTo("19.6333");
		assertThat(waiaha.longitude()).isEqualByComparingTo("-155.9489");
		assertThat(waiaha.archiveBegin()).hasToString("2012-03-20");
		assertThat(result.stations().get(1).latitude()).isNull();
	}

	/**
	 * Verifies that rejects malformed features individually.
	 * @throws IOException if an I/O operation fails
	 */
	@Test
	void rejectsMalformedFeaturesIndividually() throws IOException {
		StationCatalogResult result = client().parse(
				fixture("iem-hi-dcp-catalog-malformed.json"), "HI_DCP");

		assertThat(result.stations()).extracting(CatalogStation::stationId).containsExactly("GOODH1");
		assertThat(result.rejectedEntries()).isEqualTo(2);
		assertThat(result.warnings()).hasSize(2);
	}

	/**
	 * Creates a provider client for a test scenario.
	 * @return the resulting client
	 */
	private IemStationCatalogClient client() {
		ProviderRestClientFactory factory = mock(ProviderRestClientFactory.class);
		given(factory.create(org.mockito.ArgumentMatchers.anyString())).willReturn(mock(RestClient.class));
		return new IemStationCatalogClient(factory, new RainfallProperties(),
				new ProviderStatusRegistry(), Clock.systemUTC());
	}

	/**
	 * Loads a test fixture from the classpath.
	 * @param name the name
	 * @return the resulting fixture
	 * @throws IOException if an I/O operation fails
	 */
	private String fixture(String name) throws IOException {
		return new ClassPathResource("fixtures/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

}
