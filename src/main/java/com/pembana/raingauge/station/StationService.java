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

package com.pembana.raingauge.station;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.UnsupportedRainfallStationException;
import com.pembana.raingauge.station.client.CatalogStation;
import com.pembana.raingauge.station.client.IemStationCatalogClient;
import com.pembana.raingauge.station.client.ProviderException;
import com.pembana.raingauge.station.client.StationCatalogResult;

/**
 * Provides station operations.
 * @author Gunnar Hillert
 */
@Service
public class StationService {

	private static final Logger logger = LoggerFactory.getLogger(StationService.class);

	private final StationRepository stationRepository;

	private final IemStationCatalogClient stationCatalogClient;

	private final RainfallProperties properties;

	private final TransactionTemplate transactionTemplate;

	private final ApplicationEventPublisher eventPublisher;

	private final Clock clock;

	/**
	 * Creates a new {@code StationService}.
	 * @param stationRepository the station repository
	 * @param stationCatalogClient the station catalog client
	 * @param properties the rainfall application properties
	 * @param transactionTemplate the transaction template
	 * @param eventPublisher the event publisher
	 * @param clock the clock used to obtain the current time
	 */
	public StationService(StationRepository stationRepository,
			IemStationCatalogClient stationCatalogClient, RainfallProperties properties,
			TransactionTemplate transactionTemplate, ApplicationEventPublisher eventPublisher,
			Clock clock) {
		this.stationRepository = stationRepository;
		this.stationCatalogClient = stationCatalogClient;
		this.properties = properties;
		this.transactionTemplate = transactionTemplate;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
	}

	/**
	 * Initializes the station catalog when no stations exist.
	 * @return {@code true} when initialize catalog if empty; otherwise {@code false}
	 */
	public boolean initializeCatalogIfEmpty() {
		if (this.stationRepository.count() > 0) {
			logger.info("Station catalog bootstrap skipped because local records exist");
			return false;
		}
		try {
			refreshCatalog();
			return true;
		}
		catch (ProviderException ex) {
			if (this.properties.getCatalog().isFailStartupWhenEmpty()
					&& this.stationRepository.count() == 0) {
				throw ex;
			}
			logger.warn("Station catalog is unavailable; startup will continue with an empty catalog: {}",
					ex.getMessage());
			return false;
		}
	}

	/**
	 * Retrieves and merges the complete remote station catalog.
	 * @return the resulting refresh catalog
	 */
	public CatalogRefreshSummary refreshCatalog() {
		String network = this.properties.getCatalog().getNetwork();
		StationCatalogResult remote = this.stationCatalogClient.fetchCompleteCatalog(network);
		if (remote.stations().isEmpty()) {
			throw new ProviderException("IEM station catalog contained no usable stations");
		}
		Instant refreshedAt = this.clock.instant();
		CatalogRefreshSummary summary = this.transactionTemplate.execute((status) ->
				mergeCatalog(remote, refreshedAt));
		logger.info("Station catalog refreshed added={} updated={} unconfirmed={} rejected={} warnings={}",
				summary.added(), summary.updated(), summary.unconfirmed(), summary.rejected(),
				summary.warnings());
		this.eventPublisher.publishEvent(new StationCatalogRefreshedEvent(summary.refreshedAt()));
		return summary;
	}

	/**
	 * Runs the scheduled provider metadata refresh.
	 */
	@Scheduled(fixedDelayString = "${hawaii.rainfall.catalog.refresh-interval:24h}",
			initialDelayString = "${hawaii.rainfall.catalog.refresh-initial-delay:24h}")
	public void scheduledRefresh() {
		try {
			refreshCatalog();
		}
		catch (RuntimeException ex) {
			logger.warn("Scheduled station catalog refresh failed; existing records remain available: {}",
					ex.getMessage());
		}
	}

	/**
	 * Merges catalog.
	 * @param remote the remote
	 * @param refreshedAt the refreshed at
	 * @return the resulting merge catalog
	 */
	private CatalogRefreshSummary mergeCatalog(StationCatalogResult remote, Instant refreshedAt) {
		Map<String, Station> existing = new HashMap<>();
		for (Station station : this.stationRepository.findAll()) {
			existing.put(key(station.getNetwork(), station.getStationId()), station);
		}
		Set<String> seen = new HashSet<>();
		int added = 0;
		int updated = 0;
		for (CatalogStation source : remote.stations()) {
			String key = key(source.network(), source.stationId());
			seen.add(key);
			Station station = existing.get(key);
			if (station == null) {
				station = new Station(source.network(), source.stationId(), source.sourceName());
				existing.put(key, station);
				added++;
			}
			else {
				updated++;
			}
			station.updateSourceMetadata(sourceMetadata(source), refreshedAt);
			StationOverride override = this.properties.getStationOverrides().get(source.stationId());
			if (override != null) {
				station.applyOverride(override);
			}
		}
		int unconfirmed = 0;
		for (Map.Entry<String, Station> entry : existing.entrySet()) {
			if (!seen.contains(entry.getKey())) {
				entry.getValue().markNotSeenDuringRefresh(refreshedAt);
				unconfirmed++;
			}
		}
		this.stationRepository.saveAll(existing.values());
		return new CatalogRefreshSummary(added, updated, unconfirmed, remote.rejectedEntries(),
				remote.warnings().size(), refreshedAt);
	}

	/**
	 * Builds the stable network and station identifier key.
	 * @param network the provider network identifier
	 * @param stationId the provider station identifier
	 * @return the resulting key
	 */
	private String key(String network, String stationId) {
		return network.toUpperCase(Locale.ROOT) + ':' + stationId.toUpperCase(Locale.ROOT);
	}

	/**
	 * Maps a catalog station's provider metadata to the station entity value object.
	 * @param source the catalog station
	 * @return the source metadata
	 */
	private Station.SourceMetadata sourceMetadata(CatalogStation source) {
		return new Station.SourceMetadata(source.sourceName(), source.latitude(), source.longitude(),
				source.elevation(), source.online(), source.archiveBegin(), source.archiveEnd(),
				source.state(), source.country(), source.timeZone(), source.sourceMetadata());
	}

	/**
	 * Finds public stations.
	 * @return the matching public stations
	 */
	public List<Station> findPublicStations() {
		return this.stationRepository.findAllByEnabledTrueOrderByDisplayNameAsc();
	}

	/**
	 * Finds rainfall stations.
	 * @return the matching rainfall stations
	 */
	public List<Station> findRainfallStations() {
		return this.stationRepository.findRainfallStations(EnumSet.of(
				RainfallCapability.SUPPORTED_ACCUMULATOR,
				RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION));
	}

	/**
	 * Finds all stations.
	 * @return the matching all stations
	 */
	public List<Station> findAllStations() {
		return this.stationRepository.findAllByOrderByDisplayNameAsc();
	}

	/**
	 * Finds featured stations.
	 * @return the matching featured stations
	 */
	public List<Station> findFeaturedStations() {
		return this.stationRepository.findAllByFeaturedTrueAndEnabledTrueOrderByDisplayNameAsc();
	}

	/**
	 * Returns the required public station.
	 * @param stationId the provider station identifier
	 * @return the required public station
	 */
	public Station requirePublicStation(String stationId) {
		Station station = this.stationRepository.findByStationIdIgnoreCase(stationId)
				.orElseThrow(() -> new StationNotFoundException(stationId));
		if (!station.isEnabled()) {
			throw new StationNotFoundException(stationId);
		}
		return station;
	}

	/**
	 * Returns the required rainfall station.
	 * @param stationId the provider station identifier
	 * @return the required rainfall station
	 */
	public Station requireRainfallStation(String stationId) {
		Station station = requirePublicStation(stationId);
		if ((station.getRainfallCapability() != RainfallCapability.SUPPORTED_ACCUMULATOR
				&& station.getRainfallCapability()
						!= RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION)
				|| station.getPrecipitationKey() == null) {
			throw new UnsupportedRainfallStationException(stationId);
		}
		return station;
	}

	/**
	 * Records latest observation.
	 * @param station the station to process
	 * @param observedAt the observed at
	 */
	public void recordLatestObservation(Station station, Instant observedAt) {
		station.recordLatestObservation(observedAt);
		this.transactionTemplate.executeWithoutResult((status) -> this.stationRepository.save(station));
	}

	/**
	 * Counts persisted stations.
	 * @return the number of persisted stations
	 */
	public long count() {
		return this.stationRepository.count();
	}

	/**
	 * Describes a catalog refresh summary.
	 * @param added the added
	 * @param updated the updated
	 * @param unconfirmed the unconfirmed
	 * @param rejected the rejected
	 * @param warnings the warnings
	 * @param refreshedAt the refreshed at
	 * @author Gunnar Hillert
	 */
	public record CatalogRefreshSummary(int added, int updated, int unconfirmed, int rejected,
			int warnings, Instant refreshedAt) {
	}

}
