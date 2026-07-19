package com.pembana.raingauge.station;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.pembana.raingauge.dashboard.DashboardResponse;
import com.pembana.raingauge.dashboard.DashboardService;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallService;
import com.pembana.raingauge.rainfall.RainfallWindow;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StationController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final RainfallService rainfallService;

	private final Clock clock;

	public StationController(StationService stationService, DashboardService dashboardService,
			RainfallService rainfallService, Clock clock) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.rainfallService = rainfallService;
		this.clock = clock;
	}

	@GetMapping("/stations")
	public String stations(@RequestParam(required = false) @Nullable String query,
			@RequestParam(required = false) @Nullable String island,
			@RequestParam(required = false) @Nullable Boolean online,
			@RequestParam(required = false) @Nullable RainfallCapability capability,
			@RequestParam(required = false) @Nullable Boolean enabled,
			@RequestParam(required = false) @Nullable Boolean recent, Model model) {
		List<StationResponse> filtered = this.stationService.findAllStations().stream()
				.filter((station) -> matches(station, query, island, online, capability,
						enabled, recent))
				.map(StationResponse::from)
				.toList();
		model.addAttribute("view", new StationListView(filtered, query, island, online,
				capability, enabled, recent, this.stationService.count()));
		return "stations";
	}

	@GetMapping("/stations/{stationId}")
	public String station(@PathVariable String stationId,
			@RequestParam(required = false) @Nullable String from,
			@RequestParam(required = false) @Nullable String to,
			@RequestParam(defaultValue = "imperial") String unit, Model model) {
		Station station = this.stationService.requirePublicStation(stationId);
		DashboardResponse dashboard = null;
		RainfallResult customResult = null;
		String error = null;
		try {
			dashboard = this.dashboardService.build(station, RainfallWindow.TWENTY_EIGHT_DAYS,
					RainfallUnit.IMPERIAL);
			if (from != null && to != null && !from.isBlank() && !to.isBlank()) {
				customResult = this.rainfallService.calculate(station,
						LocalDateTime.parse(from).atZone(RainfallWindow.HAWAII).toInstant(),
						LocalDateTime.parse(to).atZone(RainfallWindow.HAWAII).toInstant(),
						RainfallUnit.fromToken(unit));
			}
		} catch (RuntimeException ex) {
			error = ex.getMessage();
		}
		model.addAttribute("view", new StationDetailView(StationResponse.from(station),
				this.stationService.findPublicStations().stream().map(StationResponse::from).toList(),
				dashboard, customResult, from, to, unit, error));
		return "stationDetail";
	}

	private boolean matches(Station station, @Nullable String query, @Nullable String island,
			@Nullable Boolean online, @Nullable RainfallCapability capability,
			@Nullable Boolean enabled, @Nullable Boolean recent) {
		if (query != null && !query.isBlank()) {
			String needle = query.toLowerCase(Locale.ROOT);
			if (!station.getStationId().toLowerCase(Locale.ROOT).contains(needle)
					&& !station.getDisplayName().toLowerCase(Locale.ROOT).contains(needle)
					&& !station.getSourceName().toLowerCase(Locale.ROOT).contains(needle)) {
				return false;
			}
		}
		return (island == null || island.isBlank() || island.equals(station.getIsland()))
				&& (online == null || online == station.isSourceOnline())
				&& (capability == null || capability == station.getRainfallCapability())
				&& (enabled == null || enabled == station.isEnabled())
				&& (recent == null || recent == (station.getLatestObservationAt() != null
						&& station.getLatestObservationAt().isAfter(
								this.clock.instant().minus(Duration.ofHours(24)))));
	}

}
