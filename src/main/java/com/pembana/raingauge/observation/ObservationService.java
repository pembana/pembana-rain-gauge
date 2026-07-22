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

package com.pembana.raingauge.observation;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.client.HadsObservationClient;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Provides observation operations.
 * @author Gunnar Hillert
 */
@Service
public class ObservationService {

	private final HadsObservationClient observationClient;

	private final Cache<ObservationQuery, ObservationBatch> recentCache;

	private final Cache<ObservationQuery, ObservationBatch> historicalCache;

	private final Cache<ObservationQuery, ObservationBatch> staleCache;

	private final Clock clock;

	/**
	 * Creates a new {@code ObservationService}.
	 * @param observationClient the observation client
	 * @param properties the rainfall application properties
	 * @param clock the clock used to obtain the current time
	 */
	public ObservationService(HadsObservationClient observationClient,
			RainfallProperties properties, Clock clock) {
		this.observationClient = observationClient;
		this.recentCache = Caffeine.newBuilder()
				.maximumSize(1_000)
				.expireAfterWrite(properties.getCache().getObservations())
				.recordStats()
				.build();
		this.historicalCache = Caffeine.newBuilder()
				.maximumSize(2_000)
				.expireAfterWrite(properties.getCache().getHistoricalObservations())
				.recordStats()
				.build();
		this.staleCache = Caffeine.newBuilder()
				.maximumSize(2_000)
				.expireAfterWrite(properties.getCache().getStaleObservations())
				.recordStats()
				.build();
		this.clock = clock;
	}

	/**
	 * Returns rainfall increments for a station.
	 * @param stationId the provider station identifier
	 * @param network the provider network identifier
	 * @param shefKey the SHEF key
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @return the resulting observations
	 */
	public ObservationBatch observations(String stationId, String network, String shefKey,
			Instant from, Instant to) {
		ObservationQuery query = new ObservationQuery(stationId, network, shefKey, from, to);
		Cache<ObservationQuery, ObservationBatch> cache = selectCache(to);
		ObservationBatch cached = findCovering(cache, query);
		if (cached != null) {
			return cached.asCached(this.clock.instant(), false, "");
		}
		try {
			ObservationBatch fetched = this.observationClient.fetch(List.of(stationId), network,
					shefKey, from, to);
			cache.put(query, fetched);
			this.staleCache.put(query, fetched);
			return fetched;
		} catch (ProviderException ex) {
			ObservationBatch stale = findCovering(this.staleCache, query);
			if (stale != null) {
				return stale.asCached(this.clock.instant(), true,
						"Live provider request failed; a stale cached response is shown");
			}
			throw ex;
		}
	}

	/**
	 * Selects the cache appropriate for the requested interval.
	 * @param to the exclusive end of the requested interval
	 * @return the resulting select cache
	 */
	private Cache<ObservationQuery, ObservationBatch> selectCache(Instant to) {
		return to.isBefore(this.clock.instant().minusSeconds(86_400))
				? this.historicalCache : this.recentCache;
	}

	/**
	 * Finds a cached observation batch covering the requested interval.
	 * @param cache the cache
	 * @param requested the requested
	 * @return the matching covering
	 */
	private ObservationBatch findCovering(Cache<ObservationQuery, ObservationBatch> cache,
			ObservationQuery requested) {
		ObservationBatch exact = cache.getIfPresent(requested);
		if (exact != null) {
			return exact;
		}
		for (Map.Entry<ObservationQuery, ObservationBatch> entry : cache.asMap().entrySet()) {
			if (entry.getKey().covers(requested)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * Describes an observation query.
	 * @param stationId the provider station identifier
	 * @param network the provider network identifier
	 * @param shefKey the SHEF key
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @author Gunnar Hillert
	 */
	private record ObservationQuery(
			String stationId,
			String network,
			String shefKey,
			Instant from,
			Instant to) {

		/**
		 * Determines whether covers.
		 * @param other the other
		 * @return {@code true} when covers; otherwise {@code false}
		 */
		boolean covers(ObservationQuery other) {
			return this.stationId.equals(other.stationId)
					&& this.network.equals(other.network)
					&& this.shefKey.equals(other.shefKey)
					&& !this.from.isAfter(other.from)
					&& !this.to.isBefore(other.to);
		}
	}

}
