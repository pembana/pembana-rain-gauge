package com.pembana.raingauge.rainfall;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum RainfallWindow {

	ONE_HOUR("1h", "Previous hour"),

	THREE_HOURS("3h", "Previous 3 hours"),

	SIX_HOURS("6h", "Previous 6 hours"),

	TWELVE_HOURS("12h", "Previous 12 hours"),

	TWENTY_FOUR_HOURS("24h", "Previous 24 hours"),

	SEVEN_DAYS("7d", "Previous 7 days"),

	TWENTY_EIGHT_DAYS("28d", "Previous 28 days"),

	MONTH_TO_DATE("mtd", "Month to date"),

	CALENDAR_MONTH("month", "Current calendar month"),

	YEAR_TO_DATE("ytd", "Year to date"),

	PREVIOUS_CALENDAR_YEAR("previous-year", "Previous calendar year");

	public static final ZoneId HAWAII = ZoneId.of("Pacific/Honolulu");

	private final String token;

	private final String label;

	RainfallWindow(String token, String label) {
		this.token = token;
		this.label = label;
	}

	public String token() {
		return this.token;
	}

	public String label() {
		return this.label;
	}

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
			case YEAR_TO_DATE -> from = LocalDate.of(localNow.getYear(), 1, 1).atStartOfDay(HAWAII);
			case PREVIOUS_CALENDAR_YEAR -> {
				from = LocalDate.of(localNow.getYear() - 1, 1, 1).atStartOfDay(HAWAII);
				to = LocalDate.of(localNow.getYear(), 1, 1).atStartOfDay(HAWAII);
			}
			default -> throw new IllegalStateException("Unhandled rainfall window " + this);
		}
		return new TimeRange(from.toInstant(), to.toInstant(), from, to);
	}

	public static TimeRange calendarMonth(YearMonth month) {
		ZonedDateTime from = month.atDay(1).atStartOfDay(HAWAII);
		ZonedDateTime to = month.plusMonths(1).atDay(1).atStartOfDay(HAWAII);
		return new TimeRange(from.toInstant(), to.toInstant(), from, to);
	}

	public static RainfallWindow fromToken(String token) {
		for (RainfallWindow window : values()) {
			if (window.token.equalsIgnoreCase(token)) {
				return window;
			}
		}
		throw new IllegalArgumentException("Unsupported rainfall period: " + token);
	}

	public record TimeRange(Instant from, Instant to, ZonedDateTime localFrom,
			ZonedDateTime localTo) {

		public Duration duration() {
			return Duration.between(this.from, this.to);
		}
	}

}
