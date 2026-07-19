package com.pembana.raingauge.dashboard;

import java.util.List;

import com.pembana.raingauge.station.StationResponse;

import org.jspecify.annotations.Nullable;

public record ComparisonView(
		List<StationResponse> availableStations,
		List<String> selectedStationIds,
		String period,
		String unit,
		@Nullable ComparisonResponse comparison,
		@Nullable String error) {

	public ComparisonView {
		availableStations = List.copyOf(availableStations);
		selectedStationIds = List.copyOf(selectedStationIds);
	}
}
