package com.pembana.raingauge.observation.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IemDailySummaryClientTests {

	@Test
	void retainsAvailableDailyValueAndDoesNotTurnMissingValueIntoZero() throws IOException {
		ProviderRestClientFactory factory = mock(ProviderRestClientFactory.class);
		when(factory.create(org.mockito.ArgumentMatchers.anyString())).thenReturn(mock(RestClient.class));
		IemDailySummaryClient client = new IemDailySummaryClient(factory, new RainfallProperties());
		String fixture = new ClassPathResource("fixtures/iem-wihh1-daily.json")
				.getContentAsString(StandardCharsets.UTF_8);

		assertThat(client.parse(fixture)).containsEntry(LocalDate.of(2026, 7, 1),
				new java.math.BigDecimal("0.01"));
		assertThat(client.parse(fixture)).doesNotContainKey(LocalDate.of(2026, 7, 2));
	}

}
