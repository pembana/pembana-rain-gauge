package com.pembana.raingauge.observation;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.client.HadsObservationClient;
import com.pembana.raingauge.station.client.ProviderException;

import org.springframework.stereotype.Service;

@Service
public class ObservationService {

	private final HadsObservationClient observationClient;

	private final Cache<ObservationQuery, ObservationBatch> recentCache;

	private final Cache<ObservationQuery, ObservationBatch> historicalCache;

	private final Cache<ObservationQuery, ObservationBatch> staleCache;

	private final Clock clock;

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

	private Cache<ObservationQuery, ObservationBatch> selectCache(Instant to) {
		return to.isBefore(this.clock.instant().minusSeconds(86_400))
				? this.historicalCache : this.recentCache;
	}

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

	private record ObservationQuery(
			String stationId,
			String network,
			String shefKey,
			Instant from,
			Instant to) {

		boolean covers(ObservationQuery other) {
			return this.stationId.equals(other.stationId)
					&& this.network.equals(other.network)
					&& this.shefKey.equals(other.shefKey)
					&& !this.from.isAfter(other.from)
					&& !this.to.isBefore(other.to);
		}
	}

}
