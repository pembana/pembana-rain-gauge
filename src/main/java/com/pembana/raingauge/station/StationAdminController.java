package com.pembana.raingauge.station;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/station-catalog")
public class StationAdminController {

	private final StationService stationService;

	public StationAdminController(StationService stationService) {
		this.stationService = stationService;
	}

	@PostMapping("/refresh")
	public ResponseEntity<StationService.CatalogRefreshSummary> refresh() {
		return ResponseEntity.ok(this.stationService.refreshCatalog());
	}

}
