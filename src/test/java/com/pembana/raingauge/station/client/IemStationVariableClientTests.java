package com.pembana.raingauge.station.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IemStationVariableClientTests {

	@Test
	void parsesActualVariableDiscoveryShape() throws IOException {
		ProviderRestClientFactory factory = mock(ProviderRestClientFactory.class);
		when(factory.create(org.mockito.ArgumentMatchers.anyString())).thenReturn(mock(RestClient.class));
		IemStationVariableClient client = new IemStationVariableClient(factory, new RainfallProperties());
		String fixture = new ClassPathResource("fixtures/iem-wihh1-vars.json")
				.getContentAsString(StandardCharsets.UTF_8);

		assertThat(client.parse(fixture)).containsExactlyInAnyOrder("PPHRGZZ", "PCIRGZZ");
	}

}
