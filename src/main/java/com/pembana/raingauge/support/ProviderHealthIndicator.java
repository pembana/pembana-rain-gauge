package com.pembana.raingauge.support;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("rainfallProviders")
public class ProviderHealthIndicator implements HealthIndicator {

	private final ProviderStatusRegistry statusRegistry;

	public ProviderHealthIndicator(ProviderStatusRegistry statusRegistry) {
		this.statusRegistry = statusRegistry;
	}

	@Override
	public Health health() {
		ProviderStatusRegistry.ProviderState catalog = this.statusRegistry.catalog();
		ProviderStatusRegistry.ProviderState observations = this.statusRegistry.observations();
		String providerStatus = (!catalog.known() && !observations.known())
				? "UNKNOWN"
				: (catalog.available() && observations.available() ? "AVAILABLE" : "DEGRADED");
		return Health.up()
				.withDetail("providerStatus", providerStatus)
				.withDetail("catalog", catalog)
				.withDetail("observations", observations)
				.build();
	}

}
