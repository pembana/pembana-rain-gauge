package com.pembana.raingauge.station.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.support.ProviderStatusRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IemStationCatalogClientTests {

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

	@Test
	void rejectsMalformedFeaturesIndividually() throws IOException {
		StationCatalogResult result = client().parse(
				fixture("iem-hi-dcp-catalog-malformed.json"), "HI_DCP");

		assertThat(result.stations()).extracting(CatalogStation::stationId).containsExactly("GOODH1");
		assertThat(result.rejectedEntries()).isEqualTo(2);
		assertThat(result.warnings()).hasSize(2);
	}

	private IemStationCatalogClient client() {
		ProviderRestClientFactory factory = mock(ProviderRestClientFactory.class);
		when(factory.create(org.mockito.ArgumentMatchers.anyString())).thenReturn(mock(RestClient.class));
		return new IemStationCatalogClient(factory, new RainfallProperties(),
				new ProviderStatusRegistry(), Clock.systemUTC());
	}

	private String fixture(String name) throws IOException {
		return new ClassPathResource("fixtures/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

}
