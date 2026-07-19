package com.pembana.raingauge.station;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "hawaii.rainfall.catalog", name = "startup-enabled",
		matchIfMissing = true)
public class StationCatalogBootstrapper implements ApplicationRunner {

	private final StationService stationService;

	public StationCatalogBootstrapper(StationService stationService) {
		this.stationService = stationService;
	}

	@Override
	public void run(ApplicationArguments args) {
		this.stationService.initializeCatalogIfEmpty();
	}

}
