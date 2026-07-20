package com.pembana.raingauge.dashboard;

import java.util.List;

import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.support.ProviderStatusRegistry;

import org.jspecify.annotations.Nullable;

public record DashboardView(
		String title,
		List<StationResponse> stations,
		@Nullable StationResponse selectedStation,
		@Nullable DashboardResponse dashboard,
		String period,
		String unit,
		@Nullable String error,
		boolean catalogEmpty,
		ProviderStatusRegistry.ProviderState catalogProvider,
		StationMap stationMap) {

	public DashboardView {
		stations = List.copyOf(stations);
	}

	public record StationMap(String tileUrl, String attributionLabel, String attributionUrl) {
	}
}
