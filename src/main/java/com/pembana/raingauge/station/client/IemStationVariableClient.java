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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;

import org.springframework.boot.json.JsonParserFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Retrieves IEM station variable data from its remote provider.
 * @author Gunnar Hillert
 */
@Component
public class IemStationVariableClient {

	private final RestClient restClient;

	/**
	 * Creates a new {@code IemStationVariableClient}.
	 * @param restClientFactory the rest client factory
	 * @param properties the rainfall application properties
	 */
	public IemStationVariableClient(ProviderRestClientFactory restClientFactory,
			RainfallProperties properties) {
		this.restClient = restClientFactory.create(properties.getProviders().getIemBaseUrl());
	}

	/**
	 * Fetches recent variables.
	 * @param stationId the provider station identifier
	 * @return the retrieved provider data
	 */
	@Cacheable(cacheNames = "stationVariables", key = "#stationId")
	public Set<String> fetchRecentVariables(String stationId) {
		try {
			String body = this.restClient.get()
					.uri((builder) -> builder.path("/json/dcp_vars.py")
							.queryParam("station", stationId).build())
					.retrieve()
					.body(String.class);
			if (body == null || body.isBlank()) {
				return Set.of();
			}
			return parse(body);
		} catch (RestClientException ex) {
			throw new ProviderException("Unable to retrieve station variables for " + stationId, ex);
		}
	}

	/**
	 * Parses advertised station variable names from an IEM JSON response.
	 * @param body the provider response body
	 * @return the parsed result
	 */
	@SuppressWarnings("unchecked")
	Set<String> parse(String body) {
		Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(body);
		Object variablesValue = root.get("vars");
		if (!(variablesValue instanceof List<?> variables)) {
			return Set.of();
		}
		Set<String> result = new LinkedHashSet<>();
		for (Object variableValue : variables) {
			if (variableValue instanceof Map<?, ?> variable) {
				Object id = ((Map<String, Object>) variable).get("id");
				if (id instanceof String string && !string.isBlank()) {
					result.add(string);
				}
			}
		}
		return Set.copyOf(result);
	}

}
