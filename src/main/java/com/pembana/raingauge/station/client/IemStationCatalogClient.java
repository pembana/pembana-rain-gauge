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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.support.ProviderStatusRegistry;

/**
 * Retrieves IEM station catalog data from its remote provider.
 * @author Gunnar Hillert
 */
@Component
public class IemStationCatalogClient {

	private static final Logger logger = LoggerFactory.getLogger(IemStationCatalogClient.class);

	private static final Pattern STATION_ID = Pattern.compile("[A-Z0-9]{3,12}");

	private final RestClient restClient;

	private final RainfallProperties properties;

	private final ProviderStatusRegistry providerStatusRegistry;

	private final Clock clock;

	/**
	 * Creates a new {@code IemStationCatalogClient}.
	 * @param restClientFactory the rest client factory
	 * @param properties the rainfall application properties
	 * @param providerStatusRegistry the provider status registry
	 * @param clock the clock used to obtain the current time
	 */
	public IemStationCatalogClient(ProviderRestClientFactory restClientFactory,
			RainfallProperties properties, ProviderStatusRegistry providerStatusRegistry, Clock clock) {
		this.restClient = restClientFactory.create(properties.getProviders().getIemBaseUrl());
		this.properties = properties;
		this.providerStatusRegistry = providerStatusRegistry;
		this.clock = clock;
	}

	/**
	 * Fetches complete catalog.
	 * @param network the provider network identifier
	 * @return the retrieved provider data
	 */
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
		}
		catch (RuntimeException ex) {
			this.providerStatusRegistry.catalogFailed(this.clock.instant(), ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Retrieves with retry.
	 * @param network the provider network identifier
	 * @return the retrieved provider data
	 */
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
			}
			catch (RestClientException ex) {
				if (attempt == attempts || !isRetryable(ex)) {
					throw new ProviderException("Unable to retrieve the IEM station catalog", ex);
				}
				backOff(attempt);
			}
		}
		throw new ProviderException("Unable to retrieve the IEM station catalog");
	}

	/**
	 * Returns whether retryable.
	 * @param exception the exception to translate
	 * @return {@code true} if retryable; otherwise {@code false}
	 */
	private boolean isRetryable(RestClientException exception) {
		if (exception instanceof RestClientResponseException responseException) {
			int status = responseException.getStatusCode().value();
			return status == 429 || status >= 500;
		}
		return true;
	}

	/**
	 * Waits for the configured retry backoff interval.
	 * @param attempt the attempt
	 */
	private void backOff(int attempt) {
		long multiplier = 1L << Math.min(attempt - 1, 8);
		long millis = this.properties.getProviders().getRetryInitialBackoff().toMillis() * multiplier;
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ProviderException("Interrupted while retrying the station catalog", ex);
		}
	}

	/**
	 * Parses a complete station catalog from an IEM GeoJSON response.
	 * @param body the provider response body
	 * @param expectedNetwork the expected network
	 * @return the parsed result
	 */
	@SuppressWarnings("unchecked")
	StationCatalogResult parse(String body, String expectedNetwork) {
		JsonParser parser = JsonParserFactory.getJsonParser();
		Map<String, Object> root;
		try {
			root = parser.parseMap(body);
		}
		catch (RuntimeException ex) {
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
			}
			catch (RuntimeException ex) {
				rejected++;
				warnings.add("Feature " + index + " rejected: " + ex.getMessage());
			}
		}
		return new StationCatalogResult(stations, warnings, rejected);
	}

	/**
	 * Maps a provider GeoJSON feature to catalog station metadata.
	 * @param feature the feature
	 * @param expectedNetwork the expected network
	 * @return the resulting map feature
	 */
	@SuppressWarnings("unchecked")
	private CatalogStation mapFeature(Map<String, Object> feature, String expectedNetwork) {
		Object propertiesValue = feature.get("properties");
		if (!(propertiesValue instanceof Map<?, ?> propertiesObject)) {
			throw new IllegalArgumentException("properties were missing");
		}
		Map<String, Object> typedProperties = (Map<String, Object>) propertiesObject;
		String stationId = requiredString(typedProperties, "sid").toUpperCase();
		if (!STATION_ID.matcher(stationId).matches()) {
			throw new IllegalArgumentException("station ID was invalid");
		}
		String network = nullableString(typedProperties.get("network"));
		if (network == null) {
			network = expectedNetwork;
		}
		if (!expectedNetwork.equals(network)) {
			throw new IllegalArgumentException("station belonged to an unexpected network");
		}
		String sourceName = requiredString(typedProperties, "sname");
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
				decimal(typedProperties.get("elevation")), date(typedProperties.get("archive_begin")),
				date(typedProperties.get("archive_end")), booleanValue(typedProperties.get("online")),
				nullableString(typedProperties.get("state")), nullableString(typedProperties.get("country")),
				nullableString(typedProperties.get("tzname")), typedProperties.toString());
	}

	/**
	 * Returns a required provider string value.
	 * @param values the values
	 * @param key the key
	 * @return the required d string
	 */
	private String requiredString(Map<String, Object> values, String key) {
		String value = nullableString(values.get(key));
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(key + " was missing");
		}
		return value;
	}

	/**
	 * Converts a provider value to a nullable string.
	 * @param value the value
	 * @return the resulting nullable string
	 */
	private @Nullable String nullableString(@Nullable Object value) {
		return (value instanceof String string && !string.isBlank()) ? string : null;
	}

	/**
	 * Converts a provider value to a decimal number.
	 * @param value the value
	 * @return the resulting decimal
	 */
	private @Nullable BigDecimal decimal(@Nullable Object value) {
		if (value instanceof Number number) {
			return new BigDecimal(number.toString());
		}
		if (value instanceof String string && !string.isBlank()) {
			try {
				return new BigDecimal(string);
			}
			catch (NumberFormatException ex) {
				logger.warn("Ignoring non-numeric station catalog value '{}': {}", string,
						ex.getMessage());
				return null;
			}
		}
		return null;
	}

	/**
	 * Converts a provider value to a local date.
	 * @param value the value
	 * @return the resulting date
	 */
	private @Nullable LocalDate date(@Nullable Object value) {
		String text = nullableString(value);
		if (text == null) {
			return null;
		}
		try {
			return LocalDate.parse(text);
		}
		catch (DateTimeParseException _) {
			return null;
		}
	}

	/**
	 * Converts a provider value to a boolean.
	 * @param value the value
	 * @return {@code true} when boolean value; otherwise {@code false}
	 */
	private boolean booleanValue(@Nullable Object value) {
		return value instanceof Boolean booleanValue && Boolean.TRUE.equals(booleanValue);
	}

}
