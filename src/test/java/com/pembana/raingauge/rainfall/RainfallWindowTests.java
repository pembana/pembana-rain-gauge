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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests rainfall window.
 * @author Gunnar Hillert
 */
class RainfallWindowTests {

	/**
	 * Verifies that previous twenty eight days means exactly six hundred seventy two hours.
	 */
	@Test
	void previousTwentyEightDaysMeansExactlySixHundredSeventyTwoHours() {
		RainfallWindow.TimeRange range = RainfallWindow.TWENTY_EIGHT_DAYS
				.resolve(Instant.parse("2026-07-18T19:00:00Z"));

		assertThat(range.duration()).isEqualTo(Duration.ofHours(28 * 24L));
	}

	/**
	 * Verifies that month to date uses hawaii local midnight.
	 */
	@Test
	void monthToDateUsesHawaiiLocalMidnight() {
		RainfallWindow.TimeRange range = RainfallWindow.MONTH_TO_DATE
				.resolve(Instant.parse("2026-07-18T19:00:00Z"));

		assertThat(range.localFrom().toLocalDate()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(range.localFrom().getOffset().toString()).isEqualTo("-10:00");
		assertThat(range.from()).isEqualTo(Instant.parse("2026-07-01T10:00:00Z"));
	}

	/**
	 * Verifies that calendar month handles leap day.
	 */
	@Test
	void calendarMonthHandlesLeapDay() {
		RainfallWindow.TimeRange range = RainfallWindow.calendarMonth(YearMonth.of(2024, 2));

		assertThat(range.localFrom().toLocalDate()).isEqualTo(LocalDate.of(2024, 2, 1));
		assertThat(range.localTo().toLocalDate()).isEqualTo(LocalDate.of(2024, 3, 1));
		assertThat(range.duration()).isEqualTo(Duration.ofDays(29));
	}

	/**
	 * Verifies that previous calendar year uses local year boundaries.
	 */
	@Test
	void previousCalendarYearUsesLocalYearBoundaries() {
		RainfallWindow.TimeRange range = RainfallWindow.PREVIOUS_CALENDAR_YEAR
				.resolve(Instant.parse("2026-07-18T19:00:00Z"));

		assertThat(range.localFrom().toLocalDate()).isEqualTo(LocalDate.of(2025, 1, 1));
		assertThat(range.localTo().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 1));
	}

	/**
	 * Verifies that conversion and presentation rounding remain separate.
	 */
	@Test
	void conversionAndPresentationRoundingRemainSeparate() {
		RainfallAmount amount = new RainfallAmount(new BigDecimal("0.05"));

		assertThat(amount.millimeters()).isEqualByComparingTo("1.270");
		assertThat(amount.display(RainfallUnit.IMPERIAL)).isEqualTo("0.05 in");
		assertThat(amount.display(RainfallUnit.METRIC)).isEqualTo("1.3 mm");
	}

}
