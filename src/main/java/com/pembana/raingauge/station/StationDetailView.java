package com.pembana.raingauge.station;

import java.util.List;

import com.pembana.raingauge.dashboard.DashboardResponse;
import com.pembana.raingauge.rainfall.RainfallResult;

import org.jspecify.annotations.Nullable;

public record StationDetailView(
		StationResponse station,
		List<StationResponse> stations,
		@Nullable DashboardResponse dashboard,
		@Nullable RainfallResult customResult,
		@Nullable String from,
		@Nullable String to,
		String unit,
		@Nullable String error) {

	public StationDetailView {
		stations = List.copyOf(stations);
	}
}
