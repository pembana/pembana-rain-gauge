package com.pembana.raingauge.dashboard;

import java.util.List;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ComparisonApiController {

	private final StationService stationService;

	private final ComparisonService comparisonService;

	public ComparisonApiController(StationService stationService,
			ComparisonService comparisonService) {
		this.stationService = stationService;
		this.comparisonService = comparisonService;
	}

	@GetMapping("/api/compare")
	public ComparisonResponse compare(@RequestParam(name = "station") List<String> stationIds,
			@RequestParam(defaultValue = "28d") String period,
			@RequestParam(defaultValue = "imperial") String unit) {
		if (stationIds.isEmpty()) {
			throw new IllegalArgumentException("At least one station must be selected");
		}
		List<Station> stations = stationIds.stream()
				.map(this.stationService::requireRainfallStation)
				.toList();
		return this.comparisonService.compare(stations, RainfallWindow.fromToken(period),
				RainfallUnit.fromToken(unit));
	}

}
