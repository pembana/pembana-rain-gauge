package com.pembana.raingauge.dashboard;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationService;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stations/{stationId}")
public class DashboardApiController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final RainfallProperties properties;

	public DashboardApiController(StationService stationService, DashboardService dashboardService,
			RainfallProperties properties) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.properties = properties;
	}

	@GetMapping("/dashboard")
	public DashboardResponse dashboard(@PathVariable String stationId,
			@RequestParam(required = false) @Nullable String period,
			@RequestParam(required = false) @Nullable String unit) {
		validateStationId(stationId);
		Station station = this.stationService.requireRainfallStation(stationId);
		RainfallWindow window = RainfallWindow.fromToken(period == null
				? this.properties.getDashboard().getDefaultPeriod() : period);
		RainfallUnit rainfallUnit = RainfallUnit.fromToken(unit == null
				? this.properties.getDashboard().getDefaultUnit() : unit);
		return this.dashboardService.build(station, window, rainfallUnit);
	}

	@GetMapping("/observations")
	public DashboardResponse.Charts observations(@PathVariable String stationId,
			@RequestParam(defaultValue = "28d") String period,
			@RequestParam(defaultValue = "imperial") String unit) {
		return dashboard(stationId, period, unit).charts();
	}

	@GetMapping("/quality-events")
	public java.util.List<String> qualityEvents(@PathVariable String stationId,
			@RequestParam(defaultValue = "28d") String period) {
		return dashboard(stationId, period, "imperial").warnings();
	}

	@GetMapping("/monthly")
	public java.util.List<DashboardResponse.DailyRainfall> monthly(@PathVariable String stationId,
			@RequestParam(defaultValue = "mtd") String period,
			@RequestParam(defaultValue = "imperial") String unit) {
		return dashboard(stationId, period, unit).dailyRainfall();
	}

	private void validateStationId(String stationId) {
		if (!stationId.matches("[A-Za-z0-9]{3,12}")) {
			throw new IllegalArgumentException("Station ID is invalid");
		}
	}

}
