package com.pembana.raingauge.station;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stations")
public class StationApiController {

	private final StationService stationService;

	public StationApiController(StationService stationService) {
		this.stationService = stationService;
	}

	@GetMapping
	public List<StationResponse> stations() {
		return this.stationService.findRainfallStations().stream().map(StationResponse::from).toList();
	}

	@GetMapping("/{stationId}")
	public StationResponse station(@PathVariable String stationId) {
		return StationResponse.from(this.stationService.requirePublicStation(stationId));
	}

}
