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

package com.pembana.raingauge.rainfall;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Enumerates the supported rainfall window values.
 * @author Gunnar Hillert
 */
public enum RainfallWindow {

	/** The hour immediately preceding the calculation time. */
	ONE_HOUR("1h", "Previous hour"),

	/** The three hours immediately preceding the calculation time. */
	THREE_HOURS("3h", "Previous 3 hours"),

	/** The six hours immediately preceding the calculation time. */
	SIX_HOURS("6h", "Previous 6 hours"),

	/** The twelve hours immediately preceding the calculation time. */
	TWELVE_HOURS("12h", "Previous 12 hours"),

	/** The twenty-four hours immediately preceding the calculation time. */
	TWENTY_FOUR_HOURS("24h", "Previous 24 hours"),

	/** The seven days immediately preceding the calculation time. */
	SEVEN_DAYS("7d", "Previous 7 days"),

	/** The twenty-eight days immediately preceding the calculation time. */
	TWENTY_EIGHT_DAYS("28d", "Previous 28 days"),

	/** The current month from local midnight on its first day. */
	MONTH_TO_DATE("mtd", "Month to date"),

	/** The current calendar month in the Hawaiʻi time zone. */
	CALENDAR_MONTH("month", "Current calendar month"),

	/** The current year from local midnight on January 1. */
	YEAR_TO_DATE("ytd", "Year to date"),

	/** The complete calendar year preceding the current local year. */
	PREVIOUS_CALENDAR_YEAR("previous-year", "Previous calendar year");

	/** Time zone used to resolve calendar-based rainfall windows. */
	public static final ZoneId HAWAII = ZoneId.of("Pacific/Honolulu");

	private final String token;

	private final String label;

	/**
	 * Creates a new {@code RainfallWindow}.
	 * @param token the token
	 * @param label the label
	 */
	RainfallWindow(String token, String label) {
		this.token = token;
		this.label = label;
	}

	/**
	 * Returns the external token for this value.
	 * @return the external token for this value
	 */
	public String token() {
		return this.token;
	}

	/**
	 * Returns the human-readable label for this window.
	 * @return the resulting label
	 */
	public String label() {
		return this.label;
	}

	/**
	 * Resolves this window relative to the supplied instant in the Hawaiʻi time zone.
	 * @param now the current instant
	 * @return the resolved time range
	 */
	public TimeRange resolve(Instant now) {
		ZonedDateTime localNow = now.atZone(HAWAII);
		ZonedDateTime from;
		ZonedDateTime to = localNow;
		switch (this) {
			case ONE_HOUR -> from = localNow.minusHours(1);
			case THREE_HOURS -> from = localNow.minusHours(3);
			case SIX_HOURS -> from = localNow.minusHours(6);
			case TWELVE_HOURS -> from = localNow.minusHours(12);
			case TWENTY_FOUR_HOURS -> from = localNow.minusHours(24);
			case SEVEN_DAYS -> from = localNow.minusHours(7 * 24L);
			case TWENTY_EIGHT_DAYS -> from = localNow.minusHours(28 * 24L);
			case MONTH_TO_DATE, CALENDAR_MONTH -> from = localNow.toLocalDate()
					.withDayOfMonth(1).atStartOfDay(HAWAII);
			case YEAR_TO_DATE -> from = LocalDate.of(localNow.getYear(), Month.JANUARY, 1).atStartOfDay(HAWAII);
			case PREVIOUS_CALENDAR_YEAR -> {
				from = LocalDate.of(localNow.getYear() - 1, Month.JANUARY, 1).atStartOfDay(HAWAII);
				to = LocalDate.of(localNow.getYear(), Month.JANUARY, 1).atStartOfDay(HAWAII);
			}
			default -> throw new IllegalStateException("Unhandled rainfall window " + this);
		}
		return new TimeRange(from.toInstant(), to.toInstant(), from, to);
	}

	/**
	 * Resolves the exact time range for a calendar month.
	 * @param month the month
	 * @return the resulting calendar month
	 */
	public static TimeRange calendarMonth(YearMonth month) {
		ZonedDateTime from = month.atDay(1).atStartOfDay(HAWAII);
		ZonedDateTime to = month.plusMonths(1).atDay(1).atStartOfDay(HAWAII);
		return new TimeRange(from.toInstant(), to.toInstant(), from, to);
	}

	/**
	 * Resolves the value represented by an external token.
	 * @param token the token
	 * @return the value represented by the supplied token or domain object
	 */
	public static RainfallWindow fromToken(String token) {
		for (RainfallWindow window : values()) {
			if (window.token.equalsIgnoreCase(token)) {
				return window;
			}
		}
		throw new IllegalArgumentException("Unsupported rainfall period: " + token);
	}

	/**
	 * Describes a time range.
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param localFrom the local from
	 * @param localTo the local to
	 * @author Gunnar Hillert
	 */
	public record TimeRange(Instant from, Instant to, ZonedDateTime localFrom,
			ZonedDateTime localTo) {

		/**
		 * Returns the duration of this time range.
		 * @return the resulting duration
		 */
		public Duration duration() {
			return Duration.between(this.from, this.to);
		}
	}

}
