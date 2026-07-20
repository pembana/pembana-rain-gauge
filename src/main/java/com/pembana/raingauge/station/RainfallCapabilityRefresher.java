package com.pembana.raingauge.station;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pembana.raingauge.config.RainfallProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RainfallCapabilityRefresher {

	private static final Logger logger = LoggerFactory.getLogger(RainfallCapabilityRefresher.class);

	private final StationRepository stationRepository;

	private final RainfallCapabilityService capabilityService;

	private final RainfallProperties properties;

	private final AtomicBoolean running = new AtomicBoolean();

	public RainfallCapabilityRefresher(StationRepository stationRepository,
			RainfallCapabilityService capabilityService, RainfallProperties properties) {
		this.stationRepository = stationRepository;
		this.capabilityService = capabilityService;
		this.properties = properties;
	}

	@EventListener
	public void applicationReady(ApplicationReadyEvent event) {
		refreshAsync();
	}

	@EventListener
	public void catalogRefreshed(StationCatalogRefreshedEvent event) {
		refreshAsync();
	}

	@Scheduled(
			fixedDelayString = "${hawaii.rainfall.catalog.capability-refresh-interval:6h}",
			initialDelayString = "${hawaii.rainfall.catalog.capability-refresh-initial-delay:6h}")
	public void scheduledRefresh() {
		refreshAsync();
	}

	public void refreshAsync() {
		if (!this.running.compareAndSet(false, true)) {
			return;
		}
		Thread.startVirtualThread(() -> {
			try {
				refresh();
			} finally {
				this.running.set(false);
			}
		});
	}

	CapabilityRefreshSummary refresh() {
		List<Station> candidates = this.stationRepository
				.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
				.filter((station) -> station.getPrecipitationKey() == null)
				.toList();
		if (candidates.isEmpty()) {
			return new CapabilityRefreshSummary(0, 0, 0, 0, 0);
		}
		int concurrency = this.properties.getCatalog().getCapabilityRefreshConcurrency();
		Thread.Builder.OfVirtual threadBuilder = Thread.ofVirtual()
				.name("rainfall-capability-", 0);
		try (ExecutorService executor = Executors.newFixedThreadPool(concurrency,
				threadBuilder.factory())) {
			List<Future<RainfallCapability>> futures = candidates.stream()
					.map((station) -> executor.submit(() -> refresh(station)))
					.toList();
			CapabilityRefreshSummary summary = summarize(futures);
			logger.info("Rainfall capabilities refreshed checked={} supported={} interval={} "
					+ "silent={} unknown={}", summary.checked(), summary.supportedAccumulators(),
					summary.intervalPrecipitation(), summary.temporarilySilent(),
					summary.unknown());
			return summary;
		}
	}

	private RainfallCapability refresh(Station station) {
		RainfallCapabilityService.CapabilityDiscovery discovery =
				this.capabilityService.discover(station);
		station.updateCapability(discovery.capability(), discovery.precipitationKey());
		this.stationRepository.save(station);
		return discovery.capability();
	}

	private CapabilityRefreshSummary summarize(List<Future<RainfallCapability>> futures) {
		int supported = 0;
		int interval = 0;
		int silent = 0;
		int unknown = 0;
		for (Future<RainfallCapability> future : futures) {
			try {
				RainfallCapability capability = future.get();
				switch (capability) {
					case SUPPORTED_ACCUMULATOR -> supported++;
					case SUPPORTED_INTERVAL_PRECIPITATION -> interval++;
					case TEMPORARILY_SILENT -> silent++;
					default -> unknown++;
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				unknown++;
			} catch (ExecutionException ex) {
				logger.warn("Rainfall capability refresh failed: {}", ex.getCause().getMessage());
				unknown++;
			}
		}
		return new CapabilityRefreshSummary(futures.size(), supported, interval, silent, unknown);
	}

	record CapabilityRefreshSummary(int checked, int supportedAccumulators,
			int intervalPrecipitation, int temporarilySilent, int unknown) {
	}

}
