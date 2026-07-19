package com.pembana.raingauge.station;

import java.util.List;

import org.jspecify.annotations.Nullable;

public record StationListView(
		List<StationResponse> stations,
		@Nullable String query,
		@Nullable String island,
		@Nullable Boolean online,
		@Nullable RainfallCapability capability,
		@Nullable Boolean enabled,
		@Nullable Boolean recent,
		long totalStations) {

	public StationListView {
		stations = List.copyOf(stations);
	}
}
