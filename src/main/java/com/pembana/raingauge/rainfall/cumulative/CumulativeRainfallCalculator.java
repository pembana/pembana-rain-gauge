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

package com.pembana.raingauge.rainfall.cumulative;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.ObservationQuality;
import com.pembana.raingauge.observation.PrecipitationObservation;
import com.pembana.raingauge.rainfall.PrecipitationObservationDeduplicator;
import com.pembana.raingauge.rainfall.RainfallAmount;
import com.pembana.raingauge.rainfall.RainfallDataQuality;
import com.pembana.raingauge.rainfall.RainfallIncrement;
import com.pembana.raingauge.rainfall.RainfallMethod;
import com.pembana.raingauge.rainfall.RainfallResult;
import com.pembana.raingauge.rainfall.RainfallResultStatus;
import com.pembana.raingauge.rainfall.RainfallUnit;

/**
 * Calculates rainfall totals from cumulative precipitation observations.
 * @author Gunnar Hillert
 */
@Component
public class CumulativeRainfallCalculator {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final RainfallProperties properties;

	private final Clock clock;

	/**
	 * Creates a new {@code CumulativeRainfallCalculator}.
	 * @param properties the rainfall application properties
	 * @param clock the clock used to obtain the current time
	 */
	public CumulativeRainfallCalculator(RainfallProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Calculates rainfall for the requested interval.
	 * @param source the source data
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @param cadence the expected observation cadence
	 * @param batch the source observation batch
	 * @param unit the requested rainfall unit
	 * @return the calculated rainfall result
	 */
	public RainfallResult calculate(List<PrecipitationObservation> source, Instant from,
			Instant to, Duration cadence, ObservationBatch batch, RainfallUnit unit) {
		Instant calculatedAt = this.clock.instant();
		PrecipitationObservationDeduplicator.Result deduplicated =
				PrecipitationObservationDeduplicator.deduplicate(source);
		List<String> warnings = new ArrayList<>(batch.warnings());
		warnings.addAll(deduplicated.warnings());
		List<PrecipitationObservation> observations = deduplicated.observations();
		CalculationRequest request = new CalculationRequest(from, to, cadence, batch, unit);
		BaselineSelection selection = selectBaseline(observations, request, warnings);
		PrecipitationObservation baseline = selection.observation();
		if (baseline == null) {
			warnings.add("No valid accumulator observations were available in the requested range");
			return unavailable(request, calculatedAt, warnings, deduplicated.conflicts());
		}
		Accumulation accumulation = accumulate(observations, baseline, request, warnings);
		return completeResult(request, baseline, selection.provisional(), calculatedAt, deduplicated,
				warnings, accumulation);
	}

	/**
	 * Creates an unavailable rainfall result with quality metadata.
	 * @param request the requested calculation
	 * @param calculatedAt the calculated at
	 * @param warnings the warnings
	 * @param conflicts the conflicts
	 * @return the resulting unavailable
	 */
	private RainfallResult unavailable(CalculationRequest request, Instant calculatedAt,
			List<String> warnings, int conflicts) {
		long expected = request.expectedSamples();
		RainfallDataQuality quality = new RainfallDataQuality(expected, 0, BigDecimal.ZERO,
				Duration.ZERO, null, null, 0, 0, conflicts, request.batch().warnings().size(),
				Duration.ZERO, request.batch().staleCache());
		return new RainfallResult(null, RainfallMethod.CUMULATIVE, request.unit(), "in",
				(request.unit() == RainfallUnit.IMPERIAL) ? 2 : 1,
				new BigDecimal("0.01"), request.from(), request.to(), null, null, null, calculatedAt,
				RainfallResultStatus.UNAVAILABLE, warnings, quality, List.of(), request.batch().provider(),
				request.batch().fetchedAt());
	}

	/**
	 * Selects a valid baseline or a provisional first observation for a calculation.
	 * @param observations the available observations
	 * @param request the requested calculation
	 * @param warnings the warnings to augment when the baseline is provisional
	 * @return the baseline selection
	 */
	private BaselineSelection selectBaseline(List<PrecipitationObservation> observations,
			CalculationRequest request, List<String> warnings) {
		PrecipitationObservation baseline = findBaseline(observations, request.from());
		if (baseline != null) {
			return new BaselineSelection(baseline, false);
		}
		PrecipitationObservation provisional = findFirstValidInWindow(observations, request.from(),
				request.to());
		if (provisional != null) {
			warnings.add("No valid accumulator baseline was available at or before the requested "
					+ "start; the total begins at " + provisional.validAt()
					+ " and may exclude earlier rainfall");
		}
		return new BaselineSelection(provisional, provisional != null);
	}

	/**
	 * Accumulates valid increments after the selected baseline.
	 * @param observations the available observations
	 * @param baseline the selected baseline observation
	 * @param request the requested calculation
	 * @param warnings the warnings to augment
	 * @return the resulting accumulation state
	 */
	private Accumulation accumulate(List<PrecipitationObservation> observations,
			PrecipitationObservation baseline, CalculationRequest request, List<String> warnings) {
		Accumulation accumulation = new Accumulation(baseline);
		for (int index = 0; index < observations.size(); index++) {
			PrecipitationObservation current = observations.get(index);
			if (isEligibleIncrement(current, baseline, request)) {
				processIncrement(observations, index, current, request, warnings, accumulation);
			}
		}
		return accumulation;
	}

	/**
	 * Determines whether an observation can contribute an accumulator increment.
	 * @param observation the candidate observation
	 * @param baseline the selected baseline observation
	 * @param request the requested calculation
	 * @return {@code true} if the observation is a valid in-range increment; otherwise
	 * {@code false}
	 */
	private boolean isEligibleIncrement(PrecipitationObservation observation,
			PrecipitationObservation baseline, CalculationRequest request) {
		return observation.validAt().isAfter(baseline.validAt())
				&& observation.validAt().isBefore(request.to())
				&& observation.quality() == ObservationQuality.VALID;
	}

	/**
	 * Processes a valid accumulator observation and records its rainfall increment.
	 * @param observations the available observations
	 * @param index the current observation index
	 * @param current the current valid observation
	 * @param request the requested calculation
	 * @param warnings the warnings to augment
	 * @param accumulation the accumulation state to update
	 */
	private void processIncrement(List<PrecipitationObservation> observations, int index,
			PrecipitationObservation current, CalculationRequest request, List<String> warnings,
			Accumulation accumulation) {
		accumulation.recordGap(current, request.from());
		BigDecimal delta = current.value().subtract(accumulation.previous().value());
		String qualityFlag = null;
		if (delta.signum() < 0) {
			NegativeDeltaResolution resolution = resolveNegativeDelta(accumulation.previous(), current,
					nextValid(observations, index + 1, request.to()), request.cadence());
			if (resolution == null) {
				accumulation.recordUnresolvedReset(current);
				warnings.add("Unresolved negative accumulator change at " + current.validAt());
				return;
			}
			delta = resolution.delta();
			qualityFlag = resolution.qualityFlag();
			accumulation.recordRecognizedReset();
			warnings.add("Accumulator " + qualityFlag + " recognized at " + current.validAt());
		}
		if (delta.compareTo(this.properties.getReset().getSuspectedOutlierIncrement()) > 0) {
			qualityFlag = "suspected-outlier";
			accumulation.recordOutlier();
			warnings.add("Unusually large positive increment at " + current.validAt());
		}
		accumulation.add(current, delta, qualityFlag, request.from());
	}

	/**
	 * Resolves a negative accumulator change as a rollover or a corroborated reset.
	 * @param previous the preceding accumulator observation
	 * @param current the current accumulator observation
	 * @param next the next valid observation, if available
	 * @param cadence the expected observation cadence
	 * @return the resolution, or {@code null} when the change cannot be classified
	 */
	private @Nullable NegativeDeltaResolution resolveNegativeDelta(
			PrecipitationObservation previous, PrecipitationObservation current,
			@Nullable PrecipitationObservation next, Duration cadence) {
		BigDecimal rollover = rolloverDelta(previous.value(), current.value());
		if (rollover != null) {
			return new NegativeDeltaResolution(rollover, "rollover");
		}
		if (isCorroboratedReset(current, next, cadence)) {
			return new NegativeDeltaResolution(current.value(), "reset");
		}
		return null;
	}

	/**
	 * Builds a completed cumulative-rainfall result from the accumulated increments.
	 * @param request the requested calculation
	 * @param baseline the selected baseline observation
	 * @param provisionalBaseline whether the baseline was taken from inside the range
	 * @param calculatedAt the calculation time
	 * @param deduplicated the deduplicated source observations
	 * @param warnings the accumulated warnings
	 * @param accumulation the accumulation state
	 * @return the completed rainfall result
	 */
	private RainfallResult completeResult(CalculationRequest request,
			PrecipitationObservation baseline, boolean provisionalBaseline, Instant calculatedAt,
			PrecipitationObservationDeduplicator.Result deduplicated, List<String> warnings,
			Accumulation accumulation) {
		List<PrecipitationObservation> inWindow = validObservationsInWindow(
				deduplicated.observations(), request);
		Instant first = inWindow.isEmpty() ? null : inWindow.getFirst().validAt();
		Instant last = inWindow.isEmpty() ? baseline.validAt() : inWindow.getLast().validAt();
		long expected = request.expectedSamples();
		long received = inWindow.size();
		BigDecimal completeness = BigDecimal.valueOf(Math.min(received, expected))
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(expected), 1, RoundingMode.HALF_UP);
		boolean materialGap = accumulation.longestGap().compareTo(request.cadence().multipliedBy(2)) > 0;
		if (materialGap) {
			warnings.add("Native observations contain a gap of "
					+ accumulation.longestGap().toMinutes() + " minutes");
		}
		Duration sourceAge = Duration.between(last, calculatedAt);
		boolean stale = sourceAge.compareTo(request.cadence().multipliedBy(2)) > 0;
		RainfallResultStatus status = status(deduplicated.conflicts(), provisionalBaseline,
				accumulation, materialGap, request.batch().staleCache(), stale);
		RainfallDataQuality quality = new RainfallDataQuality(expected, received, completeness,
				accumulation.longestGap(), first, last, accumulation.unresolvedResets(),
				accumulation.recognizedResets(), deduplicated.conflicts(),
				request.batch().warnings().size(), sourceAge, request.batch().staleCache());
		return new RainfallResult(new RainfallAmount(accumulation.total()), RainfallMethod.CUMULATIVE,
				request.unit(), "in", (request.unit() == RainfallUnit.IMPERIAL) ? 2 : 1,
				new BigDecimal("0.01"), request.from(), request.to(),
				(provisionalBaseline) ? baseline.validAt() : request.from(), last, last, calculatedAt,
				status, warnings, quality, accumulation.increments(), request.batch().provider(),
				request.batch().fetchedAt());
	}

	/**
	 * Returns valid observations that fall within the requested interval.
	 * @param observations the available observations
	 * @param request the requested calculation
	 * @return the valid in-range observations
	 */
	private List<PrecipitationObservation> validObservationsInWindow(
			List<PrecipitationObservation> observations, CalculationRequest request) {
		return observations.stream()
				.filter((observation) -> !observation.validAt().isBefore(request.from())
						&& observation.validAt().isBefore(request.to())
						&& observation.quality() == ObservationQuality.VALID)
				.toList();
	}

	/**
	 * Determines the result status from completeness, data quality, and freshness.
	 * @param conflicts the number of conflicting source timestamps
	 * @param provisionalBaseline whether the baseline was taken from inside the range
	 * @param accumulation the accumulation state
	 * @param materialGap whether a material observation gap was found
	 * @param staleCache whether a stale cached response was used
	 * @param stale whether the source observations are stale
	 * @return the derived rainfall-result status
	 */
	private RainfallResultStatus status(int conflicts, boolean provisionalBaseline,
			Accumulation accumulation, boolean materialGap, boolean staleCache, boolean stale) {
		if (conflicts > 0) {
			return RainfallResultStatus.CONFLICTING;
		}
		if (provisionalBaseline || accumulation.unresolvedResets() > 0 || materialGap
				|| accumulation.hasOutlier()) {
			return RainfallResultStatus.PARTIAL;
		}
		if (staleCache || stale) {
			return RainfallResultStatus.STALE;
		}
		return RainfallResultStatus.COMPLETE;
	}

	/**
	 * Finds baseline.
	 * @param observations the precipitation observations to process
	 * @param from the inclusive start of the requested interval
	 * @return the matching baseline
	 */
	private @Nullable PrecipitationObservation findBaseline(
			List<PrecipitationObservation> observations, Instant from) {
		PrecipitationObservation baseline = null;
		for (PrecipitationObservation observation : observations) {
			if (observation.validAt().isAfter(from)) {
				break;
			}
			if (observation.quality() == ObservationQuality.VALID) {
				baseline = observation;
			}
		}
		return baseline;
	}

	/**
	 * Finds first valid in window.
	 * @param observations the precipitation observations to process
	 * @param from the inclusive start of the requested interval
	 * @param to the exclusive end of the requested interval
	 * @return the matching first valid in window
	 */
	private @Nullable PrecipitationObservation findFirstValidInWindow(
			List<PrecipitationObservation> observations, Instant from, Instant to) {
		for (PrecipitationObservation observation : observations) {
			if (!observation.validAt().isBefore(from)
					&& observation.validAt().isBefore(to)
					&& observation.quality() == ObservationQuality.VALID) {
				return observation;
			}
		}
		return null;
	}

	/**
	 * Finds the next valid observation before the interval ends.
	 * @param observations the precipitation observations to process
	 * @param start the start
	 * @param to the exclusive end of the requested interval
	 * @return the resulting next valid
	 */
	private @Nullable PrecipitationObservation nextValid(
			List<PrecipitationObservation> observations, int start, Instant to) {
		for (int index = start; index < observations.size(); index++) {
			PrecipitationObservation observation = observations.get(index);
			if (!observation.validAt().isBefore(to)) {
				return null;
			}
			if (observation.quality() == ObservationQuality.VALID) {
				return observation;
			}
		}
		return null;
	}

	/**
	 * Calculates an accumulator delta across a recognized rollover.
	 * @param previous the previous
	 * @param current the current
	 * @return the resulting rollover delta
	 */
	private @Nullable BigDecimal rolloverDelta(BigDecimal previous, BigDecimal current) {
		BigDecimal maximum = this.properties.getReset().getRolloverMaximum();
		BigDecimal rolloverFloor = maximum.multiply(new BigDecimal("0.95"));
		if (previous.compareTo(rolloverFloor) >= 0
				&& current.compareTo(this.properties.getReset().getNearZeroThreshold()) <= 0) {
			return maximum.subtract(previous).add(current);
		}
		return null;
	}

	/**
	 * Returns whether corroborated reset.
	 * @param current the current
	 * @param next the next
	 * @param cadence the expected observation cadence
	 * @return {@code true} if corroborated reset; otherwise {@code false}
	 */
	private boolean isCorroboratedReset(PrecipitationObservation current,
			@Nullable PrecipitationObservation next, Duration cadence) {
		return current.value().compareTo(this.properties.getReset().getNearZeroThreshold()) <= 0
				&& next != null
				&& next.value().subtract(current.value()).signum() >= 0
				&& Duration.between(current.validAt(), next.validAt())
						.compareTo(cadence.multipliedBy(2)) <= 0;
	}

	private record CalculationRequest(Instant from, Instant to, Duration cadence,
			ObservationBatch batch, RainfallUnit unit) {

		/**
		 * Calculates the minimum expected number of observations for the requested interval.
		 * @return the expected sample count, with a minimum of one
		 */
		private long expectedSamples() {
			return Math.max(1, Duration.between(this.from, this.to).toSeconds()
					/ Math.max(1, this.cadence.toSeconds()));
		}

	}

	private record BaselineSelection(@Nullable PrecipitationObservation observation,
			boolean provisional) {
	}

	private record NegativeDeltaResolution(BigDecimal delta, String qualityFlag) {
	}

	private static final class Accumulation {

		private PrecipitationObservation previous;

		private BigDecimal total = BigDecimal.ZERO;

		private final List<RainfallIncrement> increments = new ArrayList<>();

		private Duration longestGap = Duration.ZERO;

		private int unresolvedResets;

		private int recognizedResets;

		private boolean outlier;

		/**
		 * Creates an accumulation beginning at the selected baseline.
		 * @param baseline the selected baseline observation
		 */
		Accumulation(PrecipitationObservation baseline) {
			this.previous = baseline;
		}

		/**
		 * Records the gap before the current observation.
		 * @param current the current observation
		 * @param from the inclusive requested range start
		 */
		void recordGap(PrecipitationObservation current, Instant from) {
			Duration gap = Duration.between(this.previous.validAt(), current.validAt());
			if (gap.compareTo(this.longestGap) > 0 && current.validAt().isAfter(from)) {
				this.longestGap = gap;
			}
		}

		/**
		 * Records a negative accumulator change that could not be resolved.
		 * @param current the observation following the unresolved change
		 */
		void recordUnresolvedReset(PrecipitationObservation current) {
			this.unresolvedResets++;
			this.previous = current;
		}

		/**
		 * Records a recognized accumulator reset or rollover.
		 */
		void recordRecognizedReset() {
			this.recognizedResets++;
		}

		/**
		 * Records a retained increment that exceeds the outlier threshold.
		 */
		void recordOutlier() {
			this.outlier = true;
		}

		/**
		 * Adds a rainfall increment and advances the current accumulator observation.
		 * @param current the current observation
		 * @param delta the calculated rainfall increment
		 * @param qualityFlag the optional increment quality flag
		 * @param from the inclusive requested range start
		 */
		void add(PrecipitationObservation current, BigDecimal delta, @Nullable String qualityFlag,
				Instant from) {
			if (current.validAt().isAfter(from)) {
				this.total = this.total.add(delta);
				this.increments.add(new RainfallIncrement(current.validAt(), delta, this.total, qualityFlag));
			}
			this.previous = current;
		}

		/**
		 * Returns the preceding accumulator observation.
		 * @return the preceding observation
		 */
		PrecipitationObservation previous() {
			return this.previous;
		}

		/**
		 * Returns the accumulated rainfall amount.
		 * @return the total rainfall amount in native units
		 */
		BigDecimal total() {
			return this.total;
		}

		/**
		 * Returns the accumulated rainfall increments.
		 * @return the rainfall increments
		 */
		List<RainfallIncrement> increments() {
			return this.increments;
		}

		/**
		 * Returns the longest recorded gap between contributing observations.
		 * @return the longest gap
		 */
		Duration longestGap() {
			return this.longestGap;
		}

		/**
		 * Returns the number of unresolved negative accumulator changes.
		 * @return the unresolved reset count
		 */
		int unresolvedResets() {
			return this.unresolvedResets;
		}

		/**
		 * Returns the number of recognized resets or rollovers.
		 * @return the recognized reset count
		 */
		int recognizedResets() {
			return this.recognizedResets;
		}

		/**
		 * Returns whether a retained increment exceeded the outlier threshold.
		 * @return {@code true} if an outlier was recorded; otherwise {@code false}
		 */
		boolean hasOutlier() {
			return this.outlier;
		}

	}

}
