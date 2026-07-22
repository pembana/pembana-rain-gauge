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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.station.client.ProviderException;

import org.springframework.boot.json.JsonParserFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Retrieves IEM daily summary data from its remote provider.
 * @author Gunnar Hillert
 */
@Component
public class IemDailySummaryClient {

	private final RestClient restClient;

	/**
	 * Creates a new {@code IemDailySummaryClient}.
	 * @param restClientFactory the rest client factory
	 * @param properties the rainfall application properties
	 */
	public IemDailySummaryClient(ProviderRestClientFactory restClientFactory,
			RainfallProperties properties) {
		this.restClient = restClientFactory.create(properties.getProviders().getIemBaseUrl());
	}

	/**
	 * Fetches daily precipitation totals for a station and month.
	 * @param network the provider network identifier
	 * @param stationId the provider station identifier
	 * @param month the month
	 * @return the retrieved provider data
	 */
	@Cacheable(cacheNames = "dailySummaries", key = "#network + ':' + #stationId + ':' + #month")
	public Map<LocalDate, BigDecimal> fetch(String network, String stationId, YearMonth month) {
		try {
			String body = this.restClient.get()
					.uri((builder) -> builder.path("/api/1/daily.json")
							.queryParam("network", network)
							.queryParam("station", stationId)
							.queryParam("year", month.getYear())
							.queryParam("month", month.getMonthValue())
							.build())
					.retrieve()
					.body(String.class);
			return body == null ? Map.of() : parse(body);
		} catch (RestClientException ex) {
			throw new ProviderException("Unable to retrieve IEM daily validation data", ex);
		}
	}

	/**
	 * Parses daily precipitation totals from an IEM JSON response.
	 * @param body the provider response body
	 * @return the parsed result
	 */
	@SuppressWarnings("unchecked")
	Map<LocalDate, BigDecimal> parse(String body) {
		Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(body);
		Object dataValue = root.get("data");
		if (!(dataValue instanceof List<?> rows)) {
			return Map.of();
		}
		Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
		for (Object rowValue : rows) {
			if (!(rowValue instanceof Map<?, ?> rowObject)) {
				continue;
			}
			Map<String, Object> row = (Map<String, Object>) rowObject;
			Object date = row.get("date");
			Object precipitation = row.get("precip");
			if (date instanceof String dateText && precipitation instanceof Number number) {
				result.put(LocalDate.parse(dateText), new BigDecimal(number.toString()));
			}
		}
		return Map.copyOf(result);
	}

}
