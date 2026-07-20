package com.pembana.raingauge.station;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.UnsupportedRainfallStationException;
import com.pembana.raingauge.station.client.CatalogStation;
import com.pembana.raingauge.station.client.IemStationCatalogClient;
import com.pembana.raingauge.station.client.ProviderException;
import com.pembana.raingauge.station.client.StationCatalogResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StationService {

	private static final Logger logger = LoggerFactory.getLogger(StationService.class);

	private final StationRepository stationRepository;

	private final IemStationCatalogClient stationCatalogClient;

	private final RainfallProperties properties;

	private final TransactionTemplate transactionTemplate;

	private final ApplicationEventPublisher eventPublisher;

	private final Clock clock;

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

	public boolean initializeCatalogIfEmpty() {
		if (this.stationRepository.count() > 0) {
			logger.info("Station catalog bootstrap skipped because local records exist");
			return false;
		}
		try {
			refreshCatalog();
			return true;
		} catch (ProviderException ex) {
			if (this.properties.getCatalog().isFailStartupWhenEmpty()
					&& this.stationRepository.count() == 0) {
				throw ex;
			}
			logger.warn("Station catalog is unavailable; startup will continue with an empty catalog: {}",
					ex.getMessage());
			return false;
		}
	}

	public CatalogRefreshSummary refreshCatalog() {
		String network = this.properties.getCatalog().getNetwork();
		StationCatalogResult remote = this.stationCatalogClient.fetchCompleteCatalog(network);
		if (remote.stations().isEmpty()) {
			throw new ProviderException("IEM station catalog contained no usable stations");
		}
		Instant refreshedAt = this.clock.instant();
		CatalogRefreshSummary summary = this.transactionTemplate.execute((status) ->
				mergeCatalog(remote, refreshedAt));
		if (summary == null) {
			throw new IllegalStateException("Catalog merge did not produce a summary");
		}
		logger.info("Station catalog refreshed added={} updated={} unconfirmed={} rejected={} warnings={}",
				summary.added(), summary.updated(), summary.unconfirmed(), summary.rejected(),
				summary.warnings());
		this.eventPublisher.publishEvent(new StationCatalogRefreshedEvent(summary.refreshedAt()));
		return summary;
	}

	@Scheduled(fixedDelayString = "${hawaii.rainfall.catalog.refresh-interval:24h}",
			initialDelayString = "${hawaii.rainfall.catalog.refresh-initial-delay:24h}")
	public void scheduledRefresh() {
		try {
			refreshCatalog();
		} catch (RuntimeException ex) {
			logger.warn("Scheduled station catalog refresh failed; existing records remain available: {}",
					ex.getMessage());
		}
	}

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
			} else {
				updated++;
			}
			station.updateSourceMetadata(source.sourceName(), source.latitude(), source.longitude(),
					source.elevation(), source.online(), source.archiveBegin(), source.archiveEnd(),
					source.state(), source.country(), source.timeZone(), source.sourceMetadata(),
					refreshedAt);
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

	private String key(String network, String stationId) {
		return network.toUpperCase(Locale.ROOT) + ':' + stationId.toUpperCase(Locale.ROOT);
	}

	public List<Station> findPublicStations() {
		return this.stationRepository.findAllByEnabledTrueOrderByDisplayNameAsc();
	}

	public List<Station> findRainfallStations() {
		return this.stationRepository.findRainfallStations(
				RainfallCapability.SUPPORTED_ACCUMULATOR);
	}

	public List<Station> findAllStations() {
		return this.stationRepository.findAllByOrderByDisplayNameAsc();
	}

	public List<Station> findFeaturedStations() {
		return this.stationRepository.findAllByFeaturedTrueAndEnabledTrueOrderByDisplayNameAsc();
	}

	public Station requirePublicStation(String stationId) {
		Station station = this.stationRepository.findByStationIdIgnoreCase(stationId)
				.orElseThrow(() -> new StationNotFoundException(stationId));
		if (!station.isEnabled()) {
			throw new StationNotFoundException(stationId);
		}
		return station;
	}

	public Station requireRainfallStation(String stationId) {
		Station station = requirePublicStation(stationId);
		if (station.getRainfallCapability() != RainfallCapability.SUPPORTED_ACCUMULATOR
				|| station.getPrecipitationKey() == null) {
			throw new UnsupportedRainfallStationException(stationId);
		}
		return station;
	}

	public void recordLatestObservation(Station station, Instant observedAt) {
		station.recordLatestObservation(observedAt);
		this.transactionTemplate.executeWithoutResult((status) -> this.stationRepository.save(station));
	}

	public long count() {
		return this.stationRepository.count();
	}

	public record CatalogRefreshSummary(int added, int updated, int unconfirmed, int rejected,
			int warnings, Instant refreshedAt) {
	}

}
