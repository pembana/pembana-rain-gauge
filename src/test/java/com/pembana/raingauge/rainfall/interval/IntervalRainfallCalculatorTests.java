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

package com.pembana.raingauge.rainfall.interval;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.PrecipitationObservation;
import com.pembana.raingauge.rainfall.RainfallMethod;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests interval rainfall calculation.
 * @author Gunnar Hillert
 */
class IntervalRainfallCalculatorTests {

	private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");

	/**
	 * Verifies adjacent interval amounts are summed without differencing.
	 */
	@Test
	void sumsAdjacentIntervals() {
		RainfallResult result = calculate(List.of(
				observation(60, "0.01", 0),
				observation(120, "0.02", 1),
				observation(180, "0.03", 2)), START, START.plus(Duration.ofHours(3)));

		assertThat(result.method()).isEqualTo(RainfallMethod.INTERVAL);
		assertThat(result.amount().inches()).isEqualByComparingTo("0.06");
		assertThat(result.amount().millimeters()).isEqualByComparingTo("1.524");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.COMPLETE);
		assertThat(result.quality().completenessPercentage()).isEqualByComparingTo("100.0");
	}

	/**
	 * Verifies the interval ending at the exclusive requested boundary is included.
	 */
	@Test
	void includesIntervalEndingAtRequestedBoundary() {
		RainfallResult result = calculate(List.of(
				observation(60, "0.01", 0),
				observation(120, "0.02", 1)), START, START.plus(Duration.ofHours(2)));

		assertThat(result.amount().inches()).isEqualByComparingTo("0.03");
		assertThat(result.increments()).hasSize(2);
	}

	/**
	 * Verifies an interval crossing the requested start is not prorated.
	 */
	@Test
	void excludesIntervalCrossingRequestedStart() {
		Instant from = START.plus(Duration.ofMinutes(30));
		RainfallResult result = calculate(List.of(
				observation(60, "0.01", 0),
				observation(120, "0.02", 1),
				observation(180, "0.03", 2)), from, START.plus(Duration.ofHours(3)));

		assertThat(result.amount().inches()).isEqualByComparingTo("0.05");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.warnings())
				.anyMatch((warning) -> warning.contains("crossing the requested start"));
	}

	/**
	 * Verifies a trailing partial interval prevents a complete result.
	 */
	@Test
	void reportsTrailingPartialInterval() {
		RainfallResult result = calculate(List.of(
				observation(60, "0.01", 0),
				observation(120, "0.02", 1)), START,
				START.plus(Duration.ofMinutes(150)));

		assertThat(result.amount().inches()).isEqualByComparingTo("0.03");
		assertThat(result.quality().completenessPercentage()).isEqualByComparingTo("100.0");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.warnings())
				.anyMatch((warning) -> warning.contains("not fully covered"));
	}

	/**
	 * Verifies rolling observations are not double-counted.
	 */
	@Test
	void excludesOverlappingIntervals() {
		RainfallResult result = calculate(List.of(
				observation(60, "0.01", 0),
				observation(90, "0.50", 1),
				observation(120, "0.03", 2)), START, START.plus(Duration.ofHours(2)));

		assertThat(result.amount().inches()).isEqualByComparingTo("0.04");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.warnings())
				.anyMatch((warning) -> warning.contains("Overlapping precipitation interval"));
	}

	/**
	 * Verifies missing intervals produce partial coverage.
	 */
	@Test
	void reportsMissingIntervalAsPartial() {
		RainfallResult result = calculate(List.of(
				observation(60, "0.01", 0),
				observation(180, "0.03", 1)), START, START.plus(Duration.ofHours(3)));

		assertThat(result.amount().inches()).isEqualByComparingTo("0.04");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.quality().completenessPercentage()).isEqualByComparingTo("66.7");
		assertThat(result.quality().longestGap()).isEqualTo(Duration.ofHours(1));
	}

	/**
	 * Verifies conflicting retransmissions are reported and never averaged.
	 */
	@Test
	void reportsConflictingRetransmissions() {
		List<PrecipitationObservation> observations = new ArrayList<>();
		observations.add(observation(60, "0.01", 0));
		observations.add(observation(60, "0.20", 1));
		observations.add(observation(120, "0.02", 2));

		RainfallResult result = calculate(observations, START,
				START.plus(Duration.ofHours(2)));

		assertThat(result.amount().inches()).isEqualByComparingTo("0.03");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.CONFLICTING);
		assertThat(result.quality().conflictingObservationCount()).isEqualTo(1);
	}

	/**
	 * Verifies negative interval precipitation is excluded.
	 */
	@Test
	void excludesNegativeIntervalAmount() {
		RainfallResult result = calculate(List.of(
				observation(60, "-0.01", 0),
				observation(120, "0.02", 1)), START, START.plus(Duration.ofHours(2)));

		assertThat(result.amount().inches()).isEqualByComparingTo("0.02");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.warnings())
				.anyMatch((warning) -> warning.contains("Negative interval precipitation"));
	}

	/**
	 * Calculates hourly interval rainfall for a test scenario.
	 * @param observations the source observations
	 * @param from the inclusive start
	 * @param to the exclusive end
	 * @return the calculated result
	 */
	private RainfallResult calculate(List<PrecipitationObservation> observations, Instant from,
			Instant to) {
		RainfallProperties properties = new RainfallProperties();
		Clock clock = Clock.fixed(to, ZoneOffset.UTC);
		IntervalRainfallCalculator calculator =
				new IntervalRainfallCalculator(properties, clock);
		ObservationBatch batch = new ObservationBatch(observations, List.of(), to,
				Duration.ZERO, false, false, "fixture", 0);
		return calculator.calculate(observations, from, to, Duration.ofHours(1), batch,
				RainfallUnit.IMPERIAL);
	}

	/**
	 * Creates an hourly interval observation ending after the supplied offset.
	 * @param minutes the ending offset in minutes
	 * @param value the interval amount in inches
	 * @param sourceOrder the source order
	 * @return the interval observation
	 */
	private PrecipitationObservation observation(int minutes, String value, int sourceOrder) {
		return PrecipitationObservation.valid("HLRH1", START.plus(Duration.ofMinutes(minutes)),
				"PPHRZ", new BigDecimal(value), sourceOrder);
	}

}
