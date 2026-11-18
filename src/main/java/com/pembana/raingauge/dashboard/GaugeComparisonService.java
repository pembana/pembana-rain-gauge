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

package com.pembana.raingauge.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.pembana.raingauge.cocorahs.DailyReport;
import com.pembana.raingauge.cocorahs.DailyReportService;
import com.pembana.raingauge.cocorahs.DailyReportsResponse;
import com.pembana.raingauge.cocorahs.DailyReportsResponse.Day;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallService;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.RainfallWindow;
import com.pembana.raingauge.rainfall.UnsupportedRainfallStationException;
import com.pembana.raingauge.station.Station;
import com.pembana.raingauge.station.StationResponse;
import com.pembana.raingauge.station.StationType;
import com.pembana.raingauge.station.client.ProviderException;

/**
 * Compares an automated gauge with an independently observed CoCoRaHS reading.
 *
 * @author OpenAI Codex
 */
@Service
public class GaugeComparisonService {

	private final DailyReportService reports;

	private final RainfallService rainfall;

	GaugeComparisonService(DailyReportService reports, RainfallService rainfall) {
		this.reports = reports;
		this.rainfall = rainfall;
	}

	Comparison compare(Station primary, List<Station> stations, String requested, ReportDates dates,
			RainfallUnit unit, @Nullable DailyReportsResponse primaryReports) {
		List<Nearby> choices = nearby(primary, stations, dates);
		if (choices.isEmpty()) {
			return new Comparison(choices, null, null,
					"No gauges from the other network have mapped locations and overlapping archive dates here.");
		}
		Nearby selected = "nearest".equals(requested) ? choices.getFirst() : choices.stream()
				.filter((choice) -> choice.station().getStationId().equalsIgnoreCase(requested)).findFirst().orElse(null);
		if (selected == null) {
			return new Comparison(choices, null, null, "Choose a gauge available for these dates.");
		}
		boolean primaryIsDaily = primary.getType() == StationType.DAILY_REPORTS;
		Station volunteer = primaryIsDaily ? primary : selected.station();
		Station automated = primaryIsDaily ? selected.station() : primary;
		try {
			DailyReportsResponse data = (primaryReports != null) ? primaryReports
					: this.reports.load(volunteer, dates.from(), dates.to(), unit, false);
			Day day = data.days().reversed().stream().filter(Day::included).findFirst().orElse(null);
			if (day == null || day.report() == null) {
				return new Comparison(choices, selected, null, data.source().staleCache()
						? "The cached CoCoRaHS reports contain no usable daily reading for these dates."
						: "This CoCoRaHS station has no usable single-day reading in the selected period.");
			}
			return readings(choices, selected, volunteer, automated, day.report(), day.display(), data, unit);
		}
		catch (ProviderException _) {
			return new Comparison(choices, selected, null, "CoCoRaHS reports are temporarily unavailable.");
		}
	}

	private Comparison readings(List<Nearby> choices, Nearby selected, Station volunteer, Station automated,
			DailyReport report, String display, DailyReportsResponse data, RainfallUnit unit) {
		LocalDate yesterday = report.end().minusDays(1);
		boolean stale = data.source().staleCache();
		LocalDateTime start = null;
		RainfallResult gauge = null;
		String note = null;
		try {
			DailyReportsResponse previous = data;
			if (yesterday.isBefore(data.from()) && yesterday.getYear() >= DailyReportService.FIRST_HAWAII_YEAR) {
				previous = this.reports.load(volunteer, yesterday, yesterday, unit, false);
				stale |= previous.source().staleCache();
			}
			start = previous.days().stream().filter((entry) -> entry.date().equals(yesterday) && entry.included())
					.map(Day::report).map(DailyReport::observedAt).findFirst().orElse(null);
			if (start == null) {
				note = "A usable reading from the previous date is needed to establish the start of this report.";
			}
		}
		catch (ProviderException _) {
			note = "The previous CoCoRaHS reading could not be retrieved.";
		}
		LocalDateTime end = report.observedAt();
		if (start != null) {
			try {
				gauge = this.rainfall.betweenReadings(automated, start.atZone(RainfallWindow.HAWAII).toInstant(),
						end.atZone(RainfallWindow.HAWAII).toInstant(), unit);
				if (gauge.increments().isEmpty()) {
					note = "The automated gauge has too few measurements inside this window to calculate a total.";
				}
			}
			catch (ProviderException | UnsupportedRainfallStationException | IllegalArgumentException _) {
				note = "Automated measurements are unavailable for this reading window.";
			}
		}
		return new Comparison(choices, selected, new Readings(StationResponse.from(volunteer),
				StationResponse.from(automated), display, start, end, stale, gauge), note);
	}

	private static List<Nearby> nearby(Station primary, List<Station> stations, ReportDates dates) {
		BigDecimal latitude = primary.getLatitude();
		BigDecimal longitude = primary.getLongitude();
		if (latitude == null || longitude == null) {
			return List.of();
		}
		List<Nearby> choices = new ArrayList<>();
		for (Station station : stations) {
			BigDecimal otherLat = station.getLatitude();
			BigDecimal otherLon = station.getLongitude();
			if (station.getType() != primary.getType() && otherLat != null && otherLon != null
					&& (station.getArchiveBegin() == null || !station.getArchiveBegin().isAfter(dates.to()))
					&& (station.getArchiveEnd() == null || !station.getArchiveEnd().isBefore(dates.from()))) {
				choices.add(new Nearby(station, distanceKm(latitude.doubleValue(), longitude.doubleValue(),
						otherLat.doubleValue(), otherLon.doubleValue())));
			}
		}
		choices.sort(Comparator.comparingDouble(Nearby::kilometers).thenComparing((choice) -> choice.station().getStationId()));
		return List.copyOf(choices);
	}

	private static double distanceKm(double lat, double lon, double otherLat, double otherLon) {
		double north = Math.sin(Math.toRadians(otherLat - lat) / 2);
		double east = Math.sin(Math.toRadians(otherLon - lon) / 2);
		double chord = north * north + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(otherLat)) * east * east;
		return 6371.0 * 2 * Math.asin(Math.min(1, Math.sqrt(chord)));
	}

	public record Nearby(Station station, double kilometers) {

		public String label(String unit) {
			String distance = "metric".equals(unit) ? String.format(Locale.US, "%.1f km", this.kilometers)
					: String.format(Locale.US, "%.1f mi", this.kilometers / 1.609344);
			return StationResponse.from(this.station).selectionLabel() + " · " + distance;
		}

	}

	public record Comparison(List<Nearby> choices, @Nullable Nearby selected, @Nullable Readings readings,
			@Nullable String note) {
	}

	public record Readings(StationResponse volunteer, StationResponse automated, String reportDisplay,
			@Nullable LocalDateTime start, LocalDateTime end, boolean staleReports, @Nullable RainfallResult gauge) {

		public RainfallResultStatus gaugeStatus() {
			if (this.gauge == null || this.gauge.increments().isEmpty()) {
				return RainfallResultStatus.UNAVAILABLE;
			}
			Instant endTime = this.end.atZone(RainfallWindow.HAWAII).toInstant();
			if (this.gauge.status() == RainfallResultStatus.COMPLETE && !endTime.equals(this.gauge.coveredEnd())) {
				return RainfallResultStatus.PARTIAL;
			}
			return this.gauge.status();
		}

		public String gaugeDisplay() {
			return (this.gauge == null || gaugeStatus() == RainfallResultStatus.UNAVAILABLE)
					? "Unavailable" : this.gauge.displayValue();
		}

	}

}
