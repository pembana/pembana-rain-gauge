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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.HadsObservationParser;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.ObservationParseResult;
import com.pembana.raingauge.station.client.ProviderException;
import com.pembana.raingauge.support.ProviderStatusRegistry;

/**
 * Retrieves HADS observation data from its remote provider.
 * @author Gunnar Hillert
 */
@Component
public class HadsObservationClient {

	private static final Logger logger = LoggerFactory.getLogger(HadsObservationClient.class);

	private final RestClient restClient;

	private final HadsObservationParser parser;

	private final RainfallProperties properties;

	private final ProviderStatusRegistry providerStatusRegistry;

	private final Clock clock;

	/**
	 * Creates a new {@code HadsObservationClient}.
	 * @param restClientFactory the rest client factory
	 * @param parser the parser
	 * @param properties the rainfall application properties
	 * @param providerStatusRegistry the provider status registry
	 * @param clock the clock used to obtain the current time
	 */
	public HadsObservationClient(ProviderRestClientFactory restClientFactory,
			HadsObservationParser parser, RainfallProperties properties,
			ProviderStatusRegistry providerStatusRegistry, Clock clock) {
		this.restClient = restClientFactory.create(properties.getProviders().getHadsBaseUrl());
		this.parser = parser;
		this.properties = properties;
		this.providerStatusRegistry = providerStatusRegistry;
		this.clock = clock;
	}

	/**
	 * Fetches HADS observations for the requested stations and interval.
	 * @param stationIds the station ids
	 * @param network the provider network identifier
	 * @param shefKey the SHEF key
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @return the retrieved provider data
	 */
	public ObservationBatch fetch(List<String> stationIds, String network, String shefKey,
			Instant from, Instant to) {
		long started = System.nanoTime();
		try {
			String body = retrieveWithRetry(stationIds, network, from, to);
			ObservationParseResult parsed = this.parser.parse(body, shefKey);
			Instant fetchedAt = this.clock.instant();
			long duration = (System.nanoTime() - started) / 1_000_000;
			this.providerStatusRegistry.observationsSucceeded(fetchedAt, duration);
			logger.info("Observation provider={} stations={} range=[{}, {}) durationMs={} parsed={} "
						+ "rejected={} warnings={}", "IEM-HADS", stationIds, from, to, duration,
					parsed.observations().size(), parsed.rejectedRows(), parsed.warnings().size());
			return new ObservationBatch(parsed.observations(), parsed.warnings(), fetchedAt,
					java.time.Duration.ZERO, false, false, "IEM HADS archive",
					parsed.rejectedRows());
		}
		catch (RuntimeException ex) {
			this.providerStatusRegistry.observationsFailed(this.clock.instant(), ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Retrieves with retry.
	 * @param stationIds the station ids
	 * @param network the provider network identifier
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @return the retrieved provider data
	 */
	private String retrieveWithRetry(List<String> stationIds, String network, Instant from, Instant to) {
		int attempts = this.properties.getProviders().getRetries() + 1;
		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				String body = this.restClient.get()
						.uri((builder) -> builder.path("/cgi-bin/request/hads.py")
								.queryParam("stations", String.join(",", stationIds))
								.queryParam("network", network)
								.queryParam("sts", from.toString())
								.queryParam("ets", to.toString())
								.queryParam("what", "txt")
								.queryParam("delim", "comma")
								.build())
						.retrieve()
						.body(String.class);
				if (body == null) {
					throw new ProviderException("HADS archive returned no response body");
				}
				if (body.getBytes(StandardCharsets.UTF_8).length
						> this.properties.getProviders().getMaximumPayloadBytes()) {
					throw new ProviderException("HADS archive response exceeded the payload limit");
				}
				return body;
			}
			catch (RestClientException ex) {
				if (attempt == attempts || !isRetryable(ex)) {
					throw new ProviderException("Unable to retrieve HADS observations", ex);
				}
				backOff(attempt);
			}
		}
		throw new ProviderException("Unable to retrieve HADS observations");
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
			throw new ProviderException("Interrupted while retrying HADS observations", ex);
		}
	}

}
