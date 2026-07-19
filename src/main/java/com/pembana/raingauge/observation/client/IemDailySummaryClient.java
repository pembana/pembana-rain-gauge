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

@Component
public class IemDailySummaryClient {

	private final RestClient restClient;

	public IemDailySummaryClient(ProviderRestClientFactory restClientFactory,
			RainfallProperties properties) {
		this.restClient = restClientFactory.create(properties.getProviders().getIemBaseUrl());
	}

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
