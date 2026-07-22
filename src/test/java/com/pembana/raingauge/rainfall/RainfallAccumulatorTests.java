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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.ObservationQuality;
import com.pembana.raingauge.observation.PrecipitationObservation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests rainfall accumulator.
 * @author Gunnar Hillert
 */
class RainfallAccumulatorTests {

	private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");

	/**
	 * Verifies that calculates normal increase from baseline.
	 */
	@Test
	void calculatesNormalIncreaseFromBaseline() {
		RainfallResult result = calculate(values("10.00", "10.02", "10.05"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.05");
		assertThat(result.amount().millimeters()).isEqualByComparingTo("1.270");
		assertThat(result.amount().display(RainfallUnit.METRIC)).isEqualTo("1.3 mm");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.COMPLETE);
	}

	/**
	 * Verifies that repeated values mean no recorded rainfall.
	 */
	@Test
	void repeatedValuesMeanNoRecordedRainfall() {
		RainfallResult result = calculate(values("7.20", "7.20", "7.20"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.increments()).extracting(RainfallIncrement::inches)
				.allMatch((amount) -> amount.compareTo(BigDecimal.ZERO) == 0);
	}

	/**
	 * Verifies that adds several increments without intermediate rounding.
	 */
	@Test
	void addsSeveralIncrementsWithoutIntermediateRounding() {
		RainfallResult result = calculate(values("1.000", "1.013", "1.027", "1.041"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.041");
	}

	/**
	 * Verifies that accepts corroborated reset to zero.
	 */
	@Test
	void acceptsCorroboratedResetToZero() {
		RainfallResult result = calculate(values("10.00", "10.05", "0.00", "0.03"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.08");
		assertThat(result.quality().recognizedResetCount()).isEqualTo(1);
		assertThat(result.quality().unresolvedResetCount()).isZero();
	}

	/**
	 * Verifies that accepts corroborated nonzero reset near zero.
	 */
	@Test
	void acceptsCorroboratedNonzeroResetNearZero() {
		RainfallResult result = calculate(values("10.00", "10.05", "0.02", "0.03"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.08");
		assertThat(result.quality().recognizedResetCount()).isEqualTo(1);
	}

	/**
	 * Verifies that recognizes configured rollover.
	 */
	@Test
	void recognizesConfiguredRollover() {
		RainfallResult result = calculate(values("99.98", "0.01", "0.03"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.05");
		assertThat(result.warnings()).anyMatch((warning) -> warning.contains("rollover"));
	}

	/**
	 * Verifies that unresolved negative delta makes result partial and is not added.
	 */
	@Test
	void unresolvedNegativeDeltaMakesResultPartialAndIsNotAdded() {
		RainfallResult result = calculate(values("10.00", "10.05", "5.00", "5.02"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.07");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.quality().unresolvedResetCount()).isEqualTo(1);
	}

	/**
	 * Verifies that collapses identical duplicates.
	 */
	@Test
	void collapsesIdenticalDuplicates() {
		List<PrecipitationObservation> observations = new ArrayList<>(values("1.00", "1.02", "1.05"));
		observations.add(observation(1, "1.02", observations.size()));

		RainfallResult result = calculate(observations, 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.05");
		assertThat(result.quality().conflictingObservationCount()).isZero();
	}

	/**
	 * Verifies that conflicting duplicates are never averaged.
	 */
	@Test
	void conflictingDuplicatesAreNeverAveraged() {
		List<PrecipitationObservation> observations = new ArrayList<>(values("1.00", "1.02", "1.05"));
		observations.add(observation(1, "1.20", observations.size()));

		RainfallResult result = calculate(observations, 3);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.CONFLICTING);
		assertThat(result.quality().conflictingObservationCount()).isEqualTo(1);
		assertThat(result.warnings()).anyMatch((warning) -> warning.contains("Conflicting"));
	}

	/**
	 * Verifies that sorts out of order observations before calculating.
	 */
	@Test
	void sortsOutOfOrderObservationsBeforeCalculating() {
		List<PrecipitationObservation> observations = List.of(
				observation(2, "3.05", 0), observation(0, "3.00", 1), observation(1, "3.02", 2));

		assertThat(calculate(observations, 3).amount().inches()).isEqualByComparingTo("0.05");
	}

	/**
	 * Verifies that material gap makes result partial rather than assuming zero.
	 */
	@Test
	void materialGapMakesResultPartialRatherThanAssumingZero() {
		List<PrecipitationObservation> observations = List.of(
				observation(0, "3.00", 0), observation(1, "3.02", 1), observation(4, "3.08", 2));

		RainfallResult result = calculate(observations, 5);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.quality().longestGap()).isEqualTo(Duration.ofHours(3));
	}

	/**
	 * Verifies that missing baseline uses first valid observation and makes result partial.
	 */
	@Test
	void missingBaselineUsesFirstValidObservationAndMakesResultPartial() {
		List<PrecipitationObservation> observations = List.of(
				observation(1, "3.02", 0), observation(2, "3.05", 1));

		RainfallResult result = calculate(observations, 3);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.amount().inches()).isEqualByComparingTo("0.03");
		assertThat(result.coveredStart()).isEqualTo(START.plus(Duration.ofHours(1)));
		assertThat(result.warnings()).anyMatch((warning) -> warning.contains("may exclude earlier"));
	}

	/**
	 * Verifies that no valid observation in range makes result unavailable.
	 */
	@Test
	void noValidObservationInRangeMakesResultUnavailable() {
		PrecipitationObservation malformed = new PrecipitationObservation("WIHH1", START, null,
				"PCIRG", "PCIRG", new BigDecimal("1.00"), ObservationQuality.MALFORMED_QUALIFIER,
				"???", null, "in", 0);

		RainfallResult result = calculate(List.of(malformed), 2);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.UNAVAILABLE);
		assertThat(result.amount()).isNull();
	}

	/**
	 * Verifies that observation exactly at ending boundary is excluded.
	 */
	@Test
	void observationExactlyAtEndingBoundaryIsExcluded() {
		RainfallResult result = calculate(values("5.00", "5.10", "5.50"), 2);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.10");
		assertThat(result.increments()).extracting(RainfallIncrement::at)
				.doesNotContain(START.plus(Duration.ofHours(2)));
	}

	/**
	 * Verifies that stale source is identified.
	 */
	@Test
	void staleSourceIsIdentified() {
		List<PrecipitationObservation> observations = values("5.00", "5.10", "5.10");
		RainfallResult result = calculate(observations, START, START.plus(Duration.ofHours(3)),
				START.plus(Duration.ofHours(8)));

		assertThat(result.status()).isEqualTo(RainfallResultStatus.STALE);
	}

	/**
	 * Verifies that malformed qualifier cannot serve as baseline but later valid observation can.
	 */
	@Test
	void malformedQualifierCannotServeAsBaselineButLaterValidObservationCan() {
		PrecipitationObservation malformed = new PrecipitationObservation("WIHH1", START, null,
				"PCIRG", "PCIRG", new BigDecimal("1.00"), ObservationQuality.MALFORMED_QUALIFIER,
				"???", null, "in", 0);
		List<PrecipitationObservation> observations = List.of(malformed, observation(1, "1.02", 1));

		RainfallResult result = calculate(observations, 2);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.amount().inches()).isZero();
		assertThat(result.coveredStart()).isEqualTo(START.plus(Duration.ofHours(1)));
	}

	/**
	 * Verifies that handles several resets in long range.
	 */
	@Test
	void handlesSeveralResetsInLongRange() {
		RainfallResult result = calculate(values("10.00", "10.04", "0.00", "0.02",
				"0.00", "0.03", "0.03"), 7);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.09");
		assertThat(result.quality().recognizedResetCount()).isEqualTo(2);
	}

	/**
	 * Verifies that suspected positive outlier is retained but flagged partial.
	 */
	@Test
	void suspectedPositiveOutlierIsRetainedButFlaggedPartial() {
		RainfallResult result = calculate(values("1.00", "7.00", "7.01"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("6.01");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.warnings()).anyMatch((warning) -> warning.contains("large positive"));
	}

	/**
	 * Calculates rainfall for the requested interval.
	 * @param observations the precipitation observations to process
	 * @param hours the hours
	 * @return the calculated rainfall result
	 */
	private RainfallResult calculate(List<PrecipitationObservation> observations, int hours) {
		return calculate(observations, START, START.plus(Duration.ofHours(hours)),
				START.plus(Duration.ofHours(hours)));
	}

	/**
	 * Calculates rainfall for the requested interval.
	 * @param observations the precipitation observations to process
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param calculatedAt the calculated at
	 * @return the calculated rainfall result
	 */
	private RainfallResult calculate(List<PrecipitationObservation> observations, Instant from,
			Instant to, Instant calculatedAt) {
		RainfallProperties properties = new RainfallProperties();
		Clock clock = Clock.fixed(calculatedAt, ZoneOffset.UTC);
		RainfallAccumulator accumulator = new RainfallAccumulator(properties, clock);
		ObservationBatch batch = new ObservationBatch(observations, List.of(), calculatedAt,
				Duration.ZERO, false, false, "fixture", 0);
		return accumulator.calculate(observations, from, to, Duration.ofHours(1), batch,
				RainfallUnit.IMPERIAL);
	}

	/**
	 * Creates ordered observations from accumulator values.
	 * @param values the values
	 * @return the resulting values
	 */
	private List<PrecipitationObservation> values(String... values) {
		List<PrecipitationObservation> observations = new ArrayList<>();
		for (int index = 0; index < values.length; index++) {
			observations.add(observation(index, values[index], index));
		}
		return observations;
	}

	/**
	 * Creates a precipitation observation for a test scenario.
	 * @param hour the hour
	 * @param value the value
	 * @param sourceOrder the source order
	 * @return the resulting observation
	 */
	private PrecipitationObservation observation(int hour, String value, int sourceOrder) {
		return PrecipitationObservation.valid("WIHH1", START.plus(Duration.ofHours(hour)),
				"PCIRG", new BigDecimal(value), sourceOrder);
	}

}
