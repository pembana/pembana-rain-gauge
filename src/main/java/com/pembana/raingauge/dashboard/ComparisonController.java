package com.pembana.raingauge.dashboard;

import java.util.ArrayList;
import java.util.List;

import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationService;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ComparisonController {

	private final StationService stationService;

	private final ComparisonService comparisonService;

	public ComparisonController(StationService stationService, ComparisonService comparisonService) {
		this.stationService = stationService;
		this.comparisonService = comparisonService;
	}

	@GetMapping("/compare")
	public String compare(@RequestParam(name = "station", required = false)
			@Nullable List<String> stationIds,
			@RequestParam(defaultValue = "28d") String period,
			@RequestParam(defaultValue = "imperial") String unit, Model model) {
		List<Station> available = this.stationService.findRainfallStations();
		List<String> selectedIds = stationIds == null ? List.of() : stationIds;
		ComparisonResponse response = null;
		String error = null;
		if (!selectedIds.isEmpty()) {
			try {
				List<Station> selected = new ArrayList<>();
				for (String stationId : selectedIds) {
					selected.add(this.stationService.requireRainfallStation(stationId));
				}
				response = this.comparisonService.compare(selected, RainfallWindow.fromToken(period),
						RainfallUnit.fromToken(unit));
			} catch (RuntimeException ex) {
				error = ex.getMessage();
			}
		}
		model.addAttribute("view", new ComparisonView(
				available.stream().map(StationResponse::from).toList(), selectedIds, period, unit,
				response, error));
		return "compare";
	}

}
