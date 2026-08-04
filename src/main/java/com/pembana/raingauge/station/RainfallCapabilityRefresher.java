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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pembana.raingauge.config.RainfallProperties;

/**
 * Refreshes station rainfall capabilities in the background.
 * @author Gunnar Hillert
 */
@Component
public class RainfallCapabilityRefresher {

	private static final Logger logger = LoggerFactory.getLogger(RainfallCapabilityRefresher.class);

	private final StationRepository stationRepository;

	private final RainfallCapabilityService capabilityService;

	private final RainfallProperties properties;

	private final AtomicBoolean running = new AtomicBoolean();

	/**
	 * Creates a new {@code RainfallCapabilityRefresher}.
	 * @param stationRepository the station repository
	 * @param capabilityService the capability service
	 * @param properties the rainfall application properties
	 */
	public RainfallCapabilityRefresher(StationRepository stationRepository,
			RainfallCapabilityService capabilityService, RainfallProperties properties) {
		this.stationRepository = stationRepository;
		this.capabilityService = capabilityService;
		this.properties = properties;
	}

	/**
	 * Starts capability discovery after the application is ready.
	 * @param event the application event
	 */
	@EventListener
	public void applicationReady(ApplicationReadyEvent event) {
		refreshAsync();
	}

	/**
	 * Starts capability discovery after a catalog refresh.
	 * @param event the application event
	 */
	@EventListener
	public void catalogRefreshed(StationCatalogRefreshedEvent event) {
		refreshAsync();
	}

	/**
	 * Runs the scheduled provider metadata refresh.
	 */
	@Scheduled(
			fixedDelayString = "${hawaii.rainfall.catalog.capability-refresh-interval:6h}",
			initialDelayString = "${hawaii.rainfall.catalog.capability-refresh-initial-delay:6h}")
	public void scheduledRefresh() {
		refreshAsync();
	}

	/**
	 * Submits a rainfall-capability refresh without blocking the caller.
	 */
	public void refreshAsync() {
		if (!this.running.compareAndSet(false, true)) {
			return;
		}
		Thread.startVirtualThread(() -> {
			try {
				refresh();
			}
			finally {
				this.running.set(false);
			}
		});
	}

	/**
	 * Refreshes rainfall capabilities for stations that have not been classified.
	 * @return the capability-refresh summary
	 */
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

	/**
	 * Discovers and persists the rainfall capability for one station.
	 * @param station the station to process
	 * @return the discovered rainfall capability
	 */
	private RainfallCapability refresh(Station station) {
		RainfallCapabilityService.CapabilityDiscovery discovery =
				this.capabilityService.discover(station);
		station.updateCapability(discovery.capability(), discovery.precipitationKey());
		this.stationRepository.save(station);
		return discovery.capability();
	}

	/**
	 * Summarizes completed capability-refresh tasks.
	 * @param futures the futures
	 * @return the resulting summarize
	 */
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
			}
			catch (InterruptedException _) {
				Thread.currentThread().interrupt();
				unknown++;
			}
			catch (ExecutionException ex) {
				Throwable cause = ex.getCause();
				logger.warn("Rainfall capability refresh failed: {}",
						(cause != null) ? cause.getMessage() : ex.getMessage());
				unknown++;
			}
		}
		return new CapabilityRefreshSummary(futures.size(), supported, interval, silent, unknown);
	}

	/**
	 * Describes a capability refresh summary.
	 * @param checked the checked
	 * @param supportedAccumulators the supported accumulators
	 * @param intervalPrecipitation the interval precipitation
	 * @param temporarilySilent the temporarily silent
	 * @param unknown the unknown
	 * @author Gunnar Hillert
	 */
	record CapabilityRefreshSummary(int checked, int supportedAccumulators,
			int intervalPrecipitation, int temporarilySilent, int unknown) {
	}

}
