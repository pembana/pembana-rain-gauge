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

@Component
public class IemStationVariableClient {

	private final RestClient restClient;

	public IemStationVariableClient(ProviderRestClientFactory restClientFactory,
			RainfallProperties properties) {
		this.restClient = restClientFactory.create(properties.getProviders().getIemBaseUrl());
	}

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
