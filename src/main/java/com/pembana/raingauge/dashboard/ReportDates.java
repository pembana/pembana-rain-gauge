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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.pembana.raingauge.rainfall.RainfallWindow;

/**
 * Inclusive Hawaiʻi observation dates for the dashboard's period selector.
 *
 * @param from first report date
 * @param to last report date
 * @author OpenAI Codex
 */
public record ReportDates(LocalDate from, LocalDate to) {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US);

	static ReportDates forWindow(RainfallWindow window, LocalDate today) {
		return switch (window) {
			case ONE_HOUR, THREE_HOURS, SIX_HOURS, TWELVE_HOURS, TWENTY_FOUR_HOURS -> new ReportDates(today, today);
			case SEVEN_DAYS -> new ReportDates(today.minusDays(6), today);
			case TWENTY_EIGHT_DAYS -> new ReportDates(today.minusDays(27), today);
			case MONTH_TO_DATE, CALENDAR_MONTH -> new ReportDates(today.withDayOfMonth(1), today);
			case YEAR_TO_DATE -> new ReportDates(today.withDayOfYear(1), today);
			case PREVIOUS_CALENDAR_YEAR -> new ReportDates(today.minusYears(1).withDayOfYear(1),
					today.withDayOfYear(1).minusDays(1));
		};
	}

	public String display() {
		return this.from.equals(this.to) ? DATE.format(this.to) : DATE.format(this.from) + " – " + DATE.format(this.to);
	}

}
