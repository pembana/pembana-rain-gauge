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

class RainfallAccumulatorTests {

	private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");

	@Test
	void calculatesNormalIncreaseFromBaseline() {
		RainfallResult result = calculate(values("10.00", "10.02", "10.05"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.05");
		assertThat(result.amount().millimeters()).isEqualByComparingTo("1.270");
		assertThat(result.amount().display(RainfallUnit.METRIC)).isEqualTo("1.3 mm");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.COMPLETE);
	}

	@Test
	void repeatedValuesMeanNoRecordedRainfall() {
		RainfallResult result = calculate(values("7.20", "7.20", "7.20"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.increments()).extracting(RainfallIncrement::inches)
				.allMatch((amount) -> amount.compareTo(BigDecimal.ZERO) == 0);
	}

	@Test
	void addsSeveralIncrementsWithoutIntermediateRounding() {
		RainfallResult result = calculate(values("1.000", "1.013", "1.027", "1.041"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.041");
	}

	@Test
	void acceptsCorroboratedResetToZero() {
		RainfallResult result = calculate(values("10.00", "10.05", "0.00", "0.03"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.08");
		assertThat(result.quality().recognizedResetCount()).isEqualTo(1);
		assertThat(result.quality().unresolvedResetCount()).isZero();
	}

	@Test
	void acceptsCorroboratedNonzeroResetNearZero() {
		RainfallResult result = calculate(values("10.00", "10.05", "0.02", "0.03"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.08");
		assertThat(result.quality().recognizedResetCount()).isEqualTo(1);
	}

	@Test
	void recognizesConfiguredRollover() {
		RainfallResult result = calculate(values("99.98", "0.01", "0.03"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.05");
		assertThat(result.warnings()).anyMatch((warning) -> warning.contains("rollover"));
	}

	@Test
	void unresolvedNegativeDeltaMakesResultPartialAndIsNotAdded() {
		RainfallResult result = calculate(values("10.00", "10.05", "5.00", "5.02"), 4);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.07");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.quality().unresolvedResetCount()).isEqualTo(1);
	}

	@Test
	void collapsesIdenticalDuplicates() {
		List<PrecipitationObservation> observations = new ArrayList<>(values("1.00", "1.02", "1.05"));
		observations.add(observation(1, "1.02", observations.size()));

		RainfallResult result = calculate(observations, 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.05");
		assertThat(result.quality().conflictingObservationCount()).isZero();
	}

	@Test
	void conflictingDuplicatesAreNeverAveraged() {
		List<PrecipitationObservation> observations = new ArrayList<>(values("1.00", "1.02", "1.05"));
		observations.add(observation(1, "1.20", observations.size()));

		RainfallResult result = calculate(observations, 3);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.CONFLICTING);
		assertThat(result.quality().conflictingObservationCount()).isEqualTo(1);
		assertThat(result.warnings()).anyMatch((warning) -> warning.contains("Conflicting"));
	}

	@Test
	void sortsOutOfOrderObservationsBeforeCalculating() {
		List<PrecipitationObservation> observations = List.of(
				observation(2, "3.05", 0), observation(0, "3.00", 1), observation(1, "3.02", 2));

		assertThat(calculate(observations, 3).amount().inches()).isEqualByComparingTo("0.05");
	}

	@Test
	void materialGapMakesResultPartialRatherThanAssumingZero() {
		List<PrecipitationObservation> observations = List.of(
				observation(0, "3.00", 0), observation(1, "3.02", 1), observation(4, "3.08", 2));

		RainfallResult result = calculate(observations, 5);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.quality().longestGap()).isEqualTo(Duration.ofHours(3));
	}

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

	@Test
	void noValidObservationInRangeMakesResultUnavailable() {
		PrecipitationObservation malformed = new PrecipitationObservation("WIHH1", START, null,
				"PCIRG", "PCIRG", new BigDecimal("1.00"), ObservationQuality.MALFORMED_QUALIFIER,
				"???", null, "in", 0);

		RainfallResult result = calculate(List.of(malformed), 2);

		assertThat(result.status()).isEqualTo(RainfallResultStatus.UNAVAILABLE);
		assertThat(result.amount()).isNull();
	}

	@Test
	void observationExactlyAtEndingBoundaryIsExcluded() {
		RainfallResult result = calculate(values("5.00", "5.10", "5.50"), 2);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.10");
		assertThat(result.increments()).extracting(RainfallIncrement::at)
				.doesNotContain(START.plus(Duration.ofHours(2)));
	}

	@Test
	void staleSourceIsIdentified() {
		List<PrecipitationObservation> observations = values("5.00", "5.10", "5.10");
		RainfallResult result = calculate(observations, START, START.plus(Duration.ofHours(3)),
				START.plus(Duration.ofHours(8)));

		assertThat(result.status()).isEqualTo(RainfallResultStatus.STALE);
	}

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

	@Test
	void handlesSeveralResetsInLongRange() {
		RainfallResult result = calculate(values("10.00", "10.04", "0.00", "0.02",
				"0.00", "0.03", "0.03"), 7);

		assertThat(result.amount().inches()).isEqualByComparingTo("0.09");
		assertThat(result.quality().recognizedResetCount()).isEqualTo(2);
	}

	@Test
	void suspectedPositiveOutlierIsRetainedButFlaggedPartial() {
		RainfallResult result = calculate(values("1.00", "7.00", "7.01"), 3);

		assertThat(result.amount().inches()).isEqualByComparingTo("6.01");
		assertThat(result.status()).isEqualTo(RainfallResultStatus.PARTIAL);
		assertThat(result.warnings()).anyMatch((warning) -> warning.contains("large positive"));
	}

	private RainfallResult calculate(List<PrecipitationObservation> observations, int hours) {
		return calculate(observations, START, START.plus(Duration.ofHours(hours)),
				START.plus(Duration.ofHours(hours)));
	}

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

	private List<PrecipitationObservation> values(String... values) {
		List<PrecipitationObservation> observations = new ArrayList<>();
		for (int index = 0; index < values.length; index++) {
			observations.add(observation(index, values[index], index));
		}
		return observations;
	}

	private PrecipitationObservation observation(int hour, String value, int sourceOrder) {
		return PrecipitationObservation.valid("WIHH1", START.plus(Duration.ofHours(hour)),
				"PCIRG", new BigDecimal(value), sourceOrder);
	}

}
