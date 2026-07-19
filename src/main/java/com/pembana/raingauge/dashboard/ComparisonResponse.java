package com.pembana.raingauge.dashboard;

import java.util.List;

import com.pembana.raingauge.station.StationResponse;

public record ComparisonResponse(String period, String unit, List<ComparisonStation> stations) {

	public ComparisonResponse {
		stations = List.copyOf(stations);
	}

	public record ComparisonStation(StationResponse station, DashboardResponse.Result total,
			List<DashboardResponse.DailyRainfall> dailyRainfall,
			List<DashboardResponse.ChartPoint> cumulativeRainfall) {

		public ComparisonStation {
			dailyRainfall = List.copyOf(dailyRainfall);
			cumulativeRainfall = List.copyOf(cumulativeRainfall);
		}
	}
}
