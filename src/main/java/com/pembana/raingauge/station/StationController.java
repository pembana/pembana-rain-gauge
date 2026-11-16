/*
 * Copyright 2026 Gunnar Hillert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pembana.raingauge.station;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.pembana.raingauge.cocorahs.DailyReportService;
import com.pembana.raingauge.cocorahs.DailyReportsResponse;
import com.pembana.raingauge.dashboard.DashboardResponse;
import com.pembana.raingauge.dashboard.DashboardService;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallService;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Handles station HTTP requests.
 * @author Gunnar Hillert
 */
@Controller
public class StationController {

	private final StationService stationService;

	private final DashboardService dashboardService;

	private final RainfallService rainfallService;

	private final Clock clock;

	private final DailyReportService dailyReports;

	/**
	 * Creates a new {@code StationController}.
	 * @param stationService the station service
	 * @param dashboardService the dashboard service
	 * @param rainfallService the rainfall service
	 * @param clock the clock used to obtain the current time
	 * @param dailyReports the CoCoRaHS rainfall service
	 */
	public StationController(StationService stationService, DashboardService dashboardService,
			RainfallService rainfallService, Clock clock, DailyReportService dailyReports) {
		this.stationService = stationService;
		this.dashboardService = dashboardService;
		this.rainfallService = rainfallService;
		this.clock = clock;
		this.dailyReports = dailyReports;
	}

	/**
	 * Returns the public station catalog.
	 * @param query the query
	 * @param network optional network filter
	 * @param island the island
	 * @param online the online
	 * @param capability the capability
	 * @param enabled the enabled
	 * @param recent the recent
	 * @param model the MVC model to populate
	 * @return the resulting stations
	 */
	@GetMapping("/stations")
	public String stations(@RequestParam(required = false) @Nullable String query,
			@RequestParam(required = false) @Nullable String network,
			@RequestParam(required = false) @Nullable String island,
			@RequestParam(required = false) @Nullable Boolean online,
			@RequestParam(required = false) @Nullable RainfallCapability capability,
			@RequestParam(required = false) @Nullable Boolean enabled,
			@RequestParam(required = false) @Nullable Boolean recent, Model model) {
		List<StationResponse> filtered = this.stationService.findAllStations().stream()
				.filter((station) -> network == null || network.isBlank() || station.getNetwork().equalsIgnoreCase(network))
				.filter((station) -> matches(station, query, island, online, capability,
						enabled, recent))
				.map(StationResponse::from)
				.toList();
		model.addAttribute("network", (network != null) ? network : "");
		model.addAttribute("view", new StationListView(filtered, query, island, online,
				capability, enabled, recent, this.stationService.count()));
		return "stations";
	}

	/**
	 * Renders rainfall or metadata for a public station.
	 * @param stationId the provider station identifier
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param unit the requested rainfall unit
	 * @param tab the requested station page tab
	 * @param csrf the form security token
	 * @param model the MVC model to populate
	 * @return the resulting station
	 */
	@GetMapping("/stations/{stationId}")
	public String station(@PathVariable String stationId,
			@RequestParam(required = false) @Nullable String from,
			@RequestParam(required = false) @Nullable String to,
			@RequestParam(defaultValue = "metric") String unit,
			@RequestParam(defaultValue = "rainfall") String tab,
			@RequestAttribute(name = "_csrf", required = false) @Nullable CsrfToken csrf, Model model) {
		Station station = this.stationService.requirePublicStation(stationId);
		if (station.getType() == StationType.DAILY_REPORTS) {
			model.addAttribute("csrf", csrf);
			return cocorahsStation(station, from, to, unit, model);
		}
		DashboardResponse dashboard = null;
		RainfallResult customResult = null;
		String error = null;
		boolean details = "details".equals(tab);
		if (!details) {
			try {
				dashboard = this.dashboardService.build(station, RainfallWindow.TWENTY_EIGHT_DAYS,
						RainfallUnit.fromToken(unit));
				if (from != null && to != null && !from.isBlank() && !to.isBlank()) {
					customResult = this.rainfallService.calculate(station,
							LocalDateTime.parse(from).atZone(RainfallWindow.HAWAII).toInstant(),
							LocalDateTime.parse(to).atZone(RainfallWindow.HAWAII).toInstant(),
							RainfallUnit.fromToken(unit));
				}
			}
			catch (RuntimeException ex) {
				error = ex.getMessage();
			}
		}
		UriComponentsBuilder stationUrl = UriComponentsBuilder.newInstance()
			.pathSegment("stations", station.getStationId())
			.queryParam("unit", unit);
		if (from != null) {
			stationUrl.queryParam("from", from);
		}
		if (to != null) {
			stationUrl.queryParam("to", to);
		}
		model.addAttribute("rainfallUrl", stationUrl.build().encode().toUriString());
		model.addAttribute("detailsUrl", stationUrl.queryParam("tab", "details")
			.build().encode().toUriString());
		model.addAttribute("details", details);
		model.addAttribute("view", new StationDetailView(StationResponse.from(station),
				dashboard, customResult, from, to, unit, error));
		return "stationDetail";
	}

	@PostMapping("/stations/{stationId}/refresh-reports")
	String refreshReports(@PathVariable String stationId, @RequestParam LocalDate from,
			@RequestParam LocalDate to, @RequestParam(required = false) @Nullable LocalDate date,
			@RequestParam(defaultValue = "metric") String unit, RedirectAttributes redirect) {
		Station station = this.stationService.requirePublicStation(stationId);
		RainfallUnit selectedUnit = RainfallUnit.fromToken(unit);
		redirect.addFlashAttribute("refreshChecks", this.dailyReports.refreshRange(station, from, to, date));
		if (date != null) {
			redirect.addFlashAttribute("refreshedDate", date);
		}
		return "redirect:" + UriComponentsBuilder.newInstance()
				.pathSegment("stations", station.getStationId()).queryParam("from", from)
				.queryParam("to", to).queryParam("unit", selectedUnit.token()).build().encode().toUriString();
	}

	private String cocorahsStation(Station station, @Nullable String from, @Nullable String to,
			String unit, Model model) {
		LocalDate today = this.dailyReports.today();
		String startText = (from != null && !from.isBlank()) ? from : today.minusDays(27).toString();
		String endText = (to != null && !to.isBlank()) ? to : today.toString();
		DailyReportsResponse reports = null;
		String latestDisplay = "";
		boolean providerUnavailable = false;
		String error = null;
		try {
			LocalDate end = LocalDate.parse(endText);
			if (from == null || from.isBlank()) {
				startText = end.minusDays(27).toString();
			}
			RainfallUnit selectedUnit = RainfallUnit.fromToken(unit);
			reports = this.dailyReports.load(station, LocalDate.parse(startText), end, selectedUnit, true);
			latestDisplay = this.dailyReports.display(reports.latestReport(), selectedUnit);
			if (model.getAttribute("refreshedDate") instanceof LocalDate date) {
				reports.days().stream().filter((day) -> day.date().equals(date)).findFirst()
						.ifPresent((day) -> model.addAttribute("refreshedReport", day));
			}
		}
		catch (DateTimeParseException _) {
			error = "Use valid observation dates.";
		}
		catch (ProviderException ex) {
			providerUnavailable = true;
			error = ex.getMessage();
		}
		catch (IllegalArgumentException ex) {
			error = ex.getMessage();
		}
		model.addAttribute("station", StationResponse.from(station));
		model.addAttribute("url", "/stations/" + station.getStationId());
		model.addAttribute("from", startText);
		model.addAttribute("to", endText);
		model.addAttribute("today", today.toString());
		model.addAttribute("unit", unit.toLowerCase(Locale.ROOT));
		model.addAttribute("reports", reports);
		model.addAttribute("canRefresh", reports != null || providerUnavailable);
		model.addAttribute("latestDisplay", latestDisplay);
		model.addAttribute("error", error);
		return "cocorahsStation";
	}

	/**
	 * Determines whether matches.
	 * @param station the station to process
	 * @param query the query
	 * @param island the island
	 * @param online the online
	 * @param capability the capability
	 * @param enabled the enabled
	 * @param recent the recent
	 * @return {@code true} when matches; otherwise {@code false}
	 */
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
		Instant latestObservationAt = station.getLatestObservationAt();
		return (island == null || island.isBlank() || island.equals(station.getIsland()))
				&& (online == null || online == station.isSourceOnline())
				&& (capability == null || capability == station.getRainfallCapability())
				&& (enabled == null || enabled == station.isEnabled())
				&& (recent == null || recent == (latestObservationAt != null
						&& latestObservationAt.isAfter(
								this.clock.instant().minus(Duration.ofHours(24)))));
	}

}
