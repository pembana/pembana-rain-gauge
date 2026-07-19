package com.pembana.raingauge.dashboard;

import java.util.ArrayList;
import java.util.List;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;

import org.springframework.stereotype.Service;

@Service
public class ComparisonService {

	private final DashboardService dashboardService;

	public ComparisonService(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	public ComparisonResponse compare(List<Station> stations, RainfallWindow window,
			RainfallUnit unit) {
		if (stations.size() > 8) {
			throw new IllegalArgumentException("No more than eight stations can be compared");
		}
		List<ComparisonResponse.ComparisonStation> results = new ArrayList<>();
		for (Station station : stations) {
			DashboardResponse dashboard = this.dashboardService.build(station, window, unit);
			results.add(new ComparisonResponse.ComparisonStation(dashboard.station(),
					selectedTotal(dashboard, window), dashboard.dailyRainfall(),
					dashboard.charts().cumulative()));
		}
		return new ComparisonResponse(window.token(), unit.token(), results);
	}

	private DashboardResponse.Result selectedTotal(DashboardResponse dashboard,
			RainfallWindow window) {
		DashboardResponse.Summary summary = dashboard.summary();
		return switch (window) {
			case ONE_HOUR -> summary.oneHour();
			case THREE_HOURS -> summary.threeHours();
			case SIX_HOURS -> summary.sixHours();
			case TWELVE_HOURS -> summary.twelveHours();
			case TWENTY_FOUR_HOURS -> summary.twentyFourHours();
			case SEVEN_DAYS -> summary.sevenDays();
			case TWENTY_EIGHT_DAYS -> summary.twentyEightDays();
			case MONTH_TO_DATE, CALENDAR_MONTH -> summary.monthToDate();
			case YEAR_TO_DATE -> summary.yearToDate();
			case PREVIOUS_CALENDAR_YEAR -> throw new IllegalArgumentException(
					"Previous-year comparison is not available in the summary response");
		};
	}

}
