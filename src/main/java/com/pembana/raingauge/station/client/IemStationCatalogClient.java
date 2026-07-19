package com.pembana.raingauge.station.client;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.support.ProviderStatusRegistry;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IemStationCatalogClient {

	private static final Logger logger = LoggerFactory.getLogger(IemStationCatalogClient.class);

	private static final Pattern STATION_ID = Pattern.compile("[A-Z0-9]{3,12}");

	private final RestClient restClient;

	private final RainfallProperties properties;

	private final ProviderStatusRegistry providerStatusRegistry;

	private final Clock clock;

	public IemStationCatalogClient(ProviderRestClientFactory restClientFactory,
			RainfallProperties properties, ProviderStatusRegistry providerStatusRegistry, Clock clock) {
		this.restClient = restClientFactory.create(properties.getProviders().getIemBaseUrl());
		this.properties = properties;
		this.providerStatusRegistry = providerStatusRegistry;
		this.clock = clock;
	}

	public StationCatalogResult fetchCompleteCatalog(String network) {
		long started = System.nanoTime();
		try {
			String body = retrieveWithRetry(network);
			StationCatalogResult result = parse(body, network);
			long duration = (System.nanoTime() - started) / 1_000_000;
			this.providerStatusRegistry.catalogSucceeded(this.clock.instant(), duration);
			logger.info("Catalog provider={} network={} durationMs={} parsed={} rejected={} warnings={}",
					"IEM", network, duration, result.stations().size(), result.rejectedEntries(),
					result.warnings().size());
			return result;
		} catch (RuntimeException ex) {
			this.providerStatusRegistry.catalogFailed(this.clock.instant(), ex.getMessage());
			throw ex;
		}
	}

	private String retrieveWithRetry(String network) {
		int attempts = this.properties.getProviders().getRetries() + 1;
		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				String body = this.restClient.get()
						.uri((builder) -> builder.path("/geojson/network.py")
								.queryParam("network", network).build())
						.retrieve()
						.body(String.class);
				if (body == null || body.isBlank()) {
					throw new ProviderException("IEM station catalog returned an empty response");
				}
				int bytes = body.getBytes(StandardCharsets.UTF_8).length;
				if (bytes > this.properties.getProviders().getMaximumPayloadBytes()) {
					throw new ProviderException(
							"IEM station catalog exceeded the configured payload limit");
				}
				return body;
			} catch (RestClientException ex) {
				if (attempt == attempts || !isRetryable(ex)) {
					throw new ProviderException("Unable to retrieve the IEM station catalog", ex);
				}
				backOff(attempt);
			}
		}
		throw new ProviderException("Unable to retrieve the IEM station catalog");
	}

	private boolean isRetryable(RestClientException exception) {
		if (exception instanceof RestClientResponseException responseException) {
			int status = responseException.getStatusCode().value();
			return status == 429 || status >= 500;
		}
		return true;
	}

	private void backOff(int attempt) {
		long multiplier = 1L << Math.min(attempt - 1, 8);
		long millis = this.properties.getProviders().getRetryInitialBackoff().toMillis() * multiplier;
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ProviderException("Interrupted while retrying the station catalog", ex);
		}
	}

	@SuppressWarnings("unchecked")
	StationCatalogResult parse(String body, String expectedNetwork) {
		JsonParser parser = JsonParserFactory.getJsonParser();
		Map<String, Object> root;
		try {
			root = parser.parseMap(body);
		} catch (RuntimeException ex) {
			throw new ProviderException("IEM station catalog was not valid JSON", ex);
		}
		Object featuresValue = root.get("features");
		if (!(featuresValue instanceof List<?> features)) {
			throw new ProviderException("IEM station catalog did not contain a features array");
		}
		List<CatalogStation> stations = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		int rejected = 0;
		for (int index = 0; index < features.size(); index++) {
			Object featureValue = features.get(index);
			try {
				if (!(featureValue instanceof Map<?, ?> feature)) {
					throw new IllegalArgumentException("feature was not an object");
				}
				CatalogStation station = mapFeature((Map<String, Object>) feature, expectedNetwork);
				stations.add(station);
			} catch (RuntimeException ex) {
				rejected++;
				warnings.add("Feature " + index + " rejected: " + ex.getMessage());
			}
		}
		return new StationCatalogResult(stations, warnings, rejected);
	}

	@SuppressWarnings("unchecked")
	private CatalogStation mapFeature(Map<String, Object> feature, String expectedNetwork) {
		Object propertiesValue = feature.get("properties");
		if (!(propertiesValue instanceof Map<?, ?> propertiesObject)) {
			throw new IllegalArgumentException("properties were missing");
		}
		Map<String, Object> properties = (Map<String, Object>) propertiesObject;
		String stationId = requiredString(properties, "sid").toUpperCase();
		if (!STATION_ID.matcher(stationId).matches()) {
			throw new IllegalArgumentException("station ID was invalid");
		}
		String network = nullableString(properties.get("network"));
		if (network == null) {
			network = expectedNetwork;
		}
		if (!expectedNetwork.equals(network)) {
			throw new IllegalArgumentException("station belonged to an unexpected network");
		}
		String sourceName = requiredString(properties, "sname");
		BigDecimal longitude = null;
		BigDecimal latitude = null;
		Object geometryValue = feature.get("geometry");
		if (geometryValue instanceof Map<?, ?> geometry) {
			Object coordinatesValue = geometry.get("coordinates");
			if (coordinatesValue instanceof List<?> coordinates && coordinates.size() >= 2) {
				longitude = decimal(coordinates.get(0));
				latitude = decimal(coordinates.get(1));
			}
		}
		return new CatalogStation(network, stationId, sourceName, latitude, longitude,
				decimal(properties.get("elevation")), date(properties.get("archive_begin")),
				date(properties.get("archive_end")), booleanValue(properties.get("online")),
				nullableString(properties.get("state")), nullableString(properties.get("country")),
				nullableString(properties.get("tzname")), properties.toString());
	}

	private String requiredString(Map<String, Object> values, String key) {
		String value = nullableString(values.get(key));
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(key + " was missing");
		}
		return value;
	}

	private @Nullable String nullableString(@Nullable Object value) {
		return value instanceof String string && !string.isBlank() ? string : null;
	}

	private @Nullable BigDecimal decimal(@Nullable Object value) {
		if (value instanceof Number number) {
			return new BigDecimal(number.toString());
		}
		if (value instanceof String string && !string.isBlank()) {
			try {
				return new BigDecimal(string);
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}

	private @Nullable LocalDate date(@Nullable Object value) {
		String text = nullableString(value);
		if (text == null) {
			return null;
		}
		try {
			return LocalDate.parse(text);
		} catch (DateTimeParseException ex) {
			return null;
		}
	}

	private boolean booleanValue(@Nullable Object value) {
		return value instanceof Boolean booleanValue && booleanValue;
	}

}
