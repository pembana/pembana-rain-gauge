package com.pembana.raingauge.station.client;

import java.util.List;

public record StationCatalogResult(
		List<CatalogStation> stations,
		List<String> warnings,
		int rejectedEntries) {

	public StationCatalogResult {
		stations = List.copyOf(stations);
		warnings = List.copyOf(warnings);
	}
}
