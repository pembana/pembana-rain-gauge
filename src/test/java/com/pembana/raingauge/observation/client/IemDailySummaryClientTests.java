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

/**
 * Tests IEM daily summary client.
 * @author Gunnar Hillert
 */
class IemDailySummaryClientTests {

	/**
	 * Verifies that retains available daily value and does not turn missing value into zero.
	 * @throws IOException if an I/O operation fails
	 */
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
