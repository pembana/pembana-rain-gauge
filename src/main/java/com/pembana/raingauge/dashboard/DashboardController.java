package com.pembana.raingauge.dashboard;

import java.util.List;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationService;
import com.pembana.raingauge.support.ProviderStatusRegistry;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final RainfallProperties properties;

	private final ProviderStatusRegistry providerStatusRegistry;

	public DashboardController(StationService stationService, DashboardService dashboardService,
			RainfallProperties properties, ProviderStatusRegistry providerStatusRegistry) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.properties = properties;
		this.providerStatusRegistry = providerStatusRegistry;
	}

	@GetMapping("/")
	public String dashboard(@RequestParam(required = false) @Nullable String station,
			@RequestParam(required = false) @Nullable String period,
			@RequestParam(required = false) @Nullable String unit, Model model) {
		List<Station> stations = this.stationService.findRainfallStations();
		String periodToken = period == null ? this.properties.getDashboard().getDefaultPeriod() : period;
		String unitToken = unit == null ? this.properties.getDashboard().getDefaultUnit() : unit;
		RainfallWindow window = RainfallWindow.fromToken(periodToken);
		RainfallUnit rainfallUnit = RainfallUnit.fromToken(unitToken);
		Station selected = select(stations, station);
		DashboardResponse response = null;
		String error = null;
		if (selected != null) {
			try {
				response = this.dashboardService.build(selected, window, rainfallUnit);
			} catch (RuntimeException ex) {
				error = "Rainfall observations are temporarily unavailable: " + ex.getMessage();
			}
		}
		DashboardView view = new DashboardView(
				selected == null ? "Pembana Rain Gauge — Hawaiʻi Rainfall Station Data"
						: selected.getDisplayName() + " rainfall — Pembana Rain Gauge",
				stations.stream().map(StationResponse::from).toList(),
				selected == null ? null : StationResponse.from(selected), response, periodToken,
				unitToken, error, stations.isEmpty(), this.providerStatusRegistry.catalog());
		model.addAttribute("view", view);
		return "dashboard";
	}

	private @Nullable Station select(List<Station> stations, @Nullable String requested) {
		if (stations.isEmpty()) {
			return null;
		}
		String stationId = requested == null
				? this.properties.getDashboard().getDefaultStation() : requested;
		return stations.stream()
				.filter((station) -> station.getStationId().equalsIgnoreCase(stationId))
				.findFirst()
				.orElseGet(() -> stations.stream().filter(Station::isFeatured).findFirst()
						.orElse(stations.getFirst()));
	}

}
