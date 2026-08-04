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
 * Calculates rainfall totals from non-overlapping interval precipitation
 * observations.
 * @author Gunnar Hillert
 */
@Component
public class IntervalRainfallCalculator {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final RainfallProperties properties;

	private final Clock clock;

	/**
	 * Creates a new {@code IntervalRainfallCalculator}.
	 * @param properties the rainfall application properties
	 * @param clock the clock used to obtain the current time
	 */
	public IntervalRainfallCalculator(RainfallProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Sums complete, non-overlapping precipitation intervals within a requested
	 * range.
	 * @param source the source observations
	 * @param from the inclusive start of the requested range
	 * @param to the exclusive end of the requested range
	 * @param interval the duration represented by each observation
	 * @param batch the source observation batch
	 * @param unit the requested rainfall unit
	 * @return the calculated rainfall result
	 */
	public RainfallResult calculate(List<PrecipitationObservation> source, Instant from,
			Instant to, Duration interval, ObservationBatch batch, RainfallUnit unit) {
		Instant calculatedAt = this.clock.instant();
		PrecipitationObservationDeduplicator.Result deduplicated =
				PrecipitationObservationDeduplicator.deduplicate(source);
		List<String> warnings = new ArrayList<>(batch.warnings());
		warnings.addAll(deduplicated.warnings());
		CalculationRequest request = new CalculationRequest(from, to, interval, batch, unit);
		IntervalAccumulation accumulation = accumulate(deduplicated.observations(), request, warnings);
		if (accumulation.hasBoundaryInterval()) {
			warnings.add("An interval crossing the requested start was excluded because it "
					+ "cannot be divided without estimating rainfall");
		}
		if (!accumulation.hasCompleteIntervals()) {
			warnings.add("No complete interval precipitation observations were available in "
					+ "the requested range");
			return unavailable(request, calculatedAt, warnings, deduplicated.conflicts());
		}
		return completeResult(request, calculatedAt, deduplicated.conflicts(), warnings, accumulation);
	}

	/**
	 * Creates an unavailable interval rainfall result with quality metadata.
	 * @param request the requested calculation
	 * @param calculatedAt the calculation time
	 * @param warnings the result warnings
	 * @param conflicts the number of conflicting timestamps
	 * @return the unavailable rainfall result
	 */
	private RainfallResult unavailable(CalculationRequest request, Instant calculatedAt,
			List<String> warnings, int conflicts) {
		long expected = request.expectedSamples();
		RainfallDataQuality quality = new RainfallDataQuality(expected, 0, BigDecimal.ZERO,
				Duration.ZERO, null, null, 0, 0, conflicts, request.batch().warnings().size(),
				Duration.ZERO, request.batch().staleCache());
		return new RainfallResult(null, RainfallMethod.INTERVAL, request.unit(), "in",
				(request.unit() == RainfallUnit.IMPERIAL) ? 2 : 1,
				new BigDecimal("0.01"), request.from(), request.to(),
				null, null, null, calculatedAt, RainfallResultStatus.UNAVAILABLE, warnings,
				quality, List.of(), request.batch().provider(), request.batch().fetchedAt());
	}

	/**
	 * Accumulates eligible intervals and their coverage metadata.
	 * @param observations the observations to examine
	 * @param request the requested calculation
	 * @param warnings the result warnings
	 * @return the accumulated interval state
	 */
	private IntervalAccumulation accumulate(List<PrecipitationObservation> observations,
			CalculationRequest request, List<String> warnings) {
		IntervalAccumulation accumulation = new IntervalAccumulation(request.from());
		for (PrecipitationObservation observation : observations) {
			if (isEligibleInterval(observation, request)) {
				processInterval(observation, request, warnings, accumulation);
			}
		}
		return accumulation;
	}

	/**
	 * Returns whether an observation ends within the requested range and is valid.
	 * @param observation the observation to examine
	 * @param request the requested calculation
	 * @return {@code true} if the interval is eligible; otherwise {@code false}
	 */
	private boolean isEligibleInterval(PrecipitationObservation observation,
			CalculationRequest request) {
		return observation.quality() == ObservationQuality.VALID
				&& observation.validAt().isAfter(request.from())
				&& !observation.validAt().isAfter(request.to());
	}

	/**
	 * Records one eligible interval or its exclusion reason.
	 * @param observation the eligible observation
	 * @param request the requested calculation
	 * @param warnings the result warnings
	 * @param accumulation the interval accumulation state
	 */
	private void processInterval(PrecipitationObservation observation, CalculationRequest request,
			List<String> warnings, IntervalAccumulation accumulation) {
		Instant intervalEnd = observation.validAt();
		Instant intervalStart = intervalEnd.minus(request.interval());
		accumulation.recordValidInterval(intervalEnd);
		if (intervalStart.isBefore(request.from())) {
			accumulation.recordBoundaryInterval();
			return;
		}
		if (observation.value().signum() < 0) {
			accumulation.recordInvalidAmount();
			warnings.add("Negative interval precipitation was excluded at " + intervalEnd);
			return;
		}
		if (intervalStart.isBefore(accumulation.coverageCursor())) {
			accumulation.recordOverlap();
			warnings.add("Overlapping precipitation interval was excluded at " + intervalEnd);
			return;
		}
		String qualityFlag = outlierQualityFlag(observation, intervalEnd, warnings, accumulation);
		accumulation.add(observation, intervalStart, qualityFlag);
	}

	/**
	 * Flags a suspiciously large interval amount.
	 * @param observation the observation to examine
	 * @param intervalEnd the end of the represented interval
	 * @param warnings the result warnings
	 * @param accumulation the interval accumulation state
	 * @return the outlier quality flag, or {@code null} if the amount is expected
	 */
	private @Nullable String outlierQualityFlag(PrecipitationObservation observation,
			Instant intervalEnd, List<String> warnings, IntervalAccumulation accumulation) {
		if (observation.value().compareTo(this.properties.getReset().getSuspectedOutlierIncrement()) > 0) {
			accumulation.recordOutlier();
			warnings.add("Unusually large interval precipitation at " + intervalEnd);
			return "suspected-outlier";
		}
		return null;
	}

	/**
	 * Creates a result from complete interval observations.
	 * @param request the requested calculation
	 * @param calculatedAt the calculation time
	 * @param conflicts the number of conflicting timestamps
	 * @param warnings the result warnings
	 * @param accumulation the interval accumulation state
	 * @return the completed rainfall result
	 */
	private RainfallResult completeResult(CalculationRequest request, Instant calculatedAt,
			int conflicts, List<String> warnings, IntervalAccumulation accumulation) {
		boolean incompleteBoundaryCoverage = accumulation.hasIncompleteBoundaryCoverage(request);
		if (incompleteBoundaryCoverage) {
			warnings.add("Requested range is not fully covered by complete precipitation "
					+ "intervals");
		}
		accumulation.recordTrailingGap(request.to());
		long expected = request.expectedSamples();
		BigDecimal completeness = BigDecimal.valueOf(Math.min(accumulation.received(), expected))
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(expected), 1, RoundingMode.HALF_UP);
		boolean materialGap = accumulation.longestGap().compareTo(request.interval()) >= 0;
		if (materialGap) {
			warnings.add("Interval observations leave an uncovered gap of "
					+ accumulation.longestGap().toMinutes() + " minutes");
		}
		Duration sourceAge = Duration.between(accumulation.lastValid(), calculatedAt);
		boolean stale = sourceAge.compareTo(request.interval().multipliedBy(2)) > 0;
		RainfallResultStatus status = status(conflicts, accumulation, incompleteBoundaryCoverage,
				materialGap, accumulation.received() < expected, request.batch().staleCache(), stale);
		RainfallDataQuality quality = new RainfallDataQuality(expected, accumulation.received(),
				completeness, accumulation.longestGap(), accumulation.firstValid(), accumulation.lastValid(),
				0, 0, conflicts, request.batch().warnings().size(), sourceAge,
				request.batch().staleCache());
		return new RainfallResult(new RainfallAmount(accumulation.total()), RainfallMethod.INTERVAL,
				request.unit(), "in", (request.unit() == RainfallUnit.IMPERIAL) ? 2 : 1,
				new BigDecimal("0.01"), request.from(), request.to(), accumulation.coveredStart(),
				accumulation.coveredEnd(), accumulation.lastValid(), calculatedAt, status, warnings,
				quality, accumulation.increments(), request.batch().provider(), request.batch().fetchedAt());
	}

	/**
	 * Determines the result status from interval coverage and source freshness.
	 * @param conflicts the number of conflicting timestamps
	 * @param accumulation the interval accumulation state
	 * @param incompleteBoundaryCoverage whether the requested boundaries are uncovered
	 * @param materialGap whether an uncovered gap is material
	 * @param incompleteSamples whether fewer samples than expected were received
	 * @param staleCache whether a stale cache was used
	 * @param stale whether the source observations are stale
	 * @return the calculated status
	 */
	private RainfallResultStatus status(int conflicts, IntervalAccumulation accumulation,
			boolean incompleteBoundaryCoverage, boolean materialGap, boolean incompleteSamples,
			boolean staleCache, boolean stale) {
		if (conflicts > 0) {
			return RainfallResultStatus.CONFLICTING;
		}
		if (accumulation.hasPartialIntervals() || incompleteBoundaryCoverage || materialGap
				|| incompleteSamples) {
			return RainfallResultStatus.PARTIAL;
		}
		if (staleCache || stale) {
			return RainfallResultStatus.STALE;
		}
		return RainfallResultStatus.COMPLETE;
	}

	private record CalculationRequest(Instant from, Instant to, Duration interval,
			ObservationBatch batch, RainfallUnit unit) {

		/**
		 * Calculates the number of observations expected in the requested range.
		 * @return the expected sample count, with a minimum of one
		 */
		private long expectedSamples() {
			return Math.max(1, Duration.between(this.from, this.to).toSeconds()
					/ Math.max(1, this.interval.toSeconds()));
		}

	}

	private static final class IntervalAccumulation {

		private BigDecimal total = BigDecimal.ZERO;

		private final List<RainfallIncrement> increments = new ArrayList<>();

		private Instant coverageCursor;

		private @Nullable Instant coveredStart;

		private @Nullable Instant coveredEnd;

		private @Nullable Instant firstValid;

		private @Nullable Instant lastValid;

		private Duration longestGap = Duration.ZERO;

		private boolean boundaryInterval;

		private boolean overlap;

		private boolean invalidAmount;

		private boolean outlier;

		private int received;

		/**
		 * Creates an accumulation beginning at the requested range start.
		 * @param from the inclusive requested range start
		 */
		IntervalAccumulation(Instant from) {
			this.coverageCursor = from;
		}

		/**
		 * Records a valid interval, including one that is later excluded.
		 * @param intervalEnd the end of the valid interval
		 */
		void recordValidInterval(Instant intervalEnd) {
			if (this.firstValid == null) {
				this.firstValid = intervalEnd;
			}
			this.lastValid = intervalEnd;
		}

		/**
		 * Records an interval crossing the requested start boundary.
		 */
		void recordBoundaryInterval() {
			this.boundaryInterval = true;
		}

		/**
		 * Records an excluded negative precipitation amount.
		 */
		void recordInvalidAmount() {
			this.invalidAmount = true;
		}

		/**
		 * Records an excluded overlapping interval.
		 */
		void recordOverlap() {
			this.overlap = true;
		}

		/**
		 * Records a suspiciously large but retained interval amount.
		 */
		void recordOutlier() {
			this.outlier = true;
		}

		/**
		 * Adds a non-overlapping interval to the accumulated rainfall.
		 * @param observation the accepted interval observation
		 * @param intervalStart the start of the represented interval
		 * @param qualityFlag the optional interval quality flag
		 */
		void add(PrecipitationObservation observation, Instant intervalStart,
				@Nullable String qualityFlag) {
			Duration gap = Duration.between(this.coverageCursor, intervalStart);
			if (gap.compareTo(this.longestGap) > 0) {
				this.longestGap = gap;
			}
			this.total = this.total.add(observation.value());
			this.increments.add(new RainfallIncrement(observation.validAt(), observation.value(),
					this.total, qualityFlag));
			this.received++;
			if (this.coveredStart == null) {
				this.coveredStart = intervalStart;
			}
			this.coveredEnd = observation.validAt();
			this.coverageCursor = observation.validAt();
		}

		/**
		 * Includes the requested range's trailing uncovered period in gap tracking.
		 * @param requestedEnd the exclusive requested range end
		 */
		void recordTrailingGap(Instant requestedEnd) {
			Duration trailingGap = Duration.between(this.coverageCursor, requestedEnd);
			if (trailingGap.compareTo(this.longestGap) > 0) {
				this.longestGap = trailingGap;
			}
		}

		/**
		 * Returns whether an interval crossed the requested start boundary.
		 * @return {@code true} if a boundary interval was excluded; otherwise {@code false}
		 */
		boolean hasBoundaryInterval() {
			return this.boundaryInterval;
		}

		/**
		 * Returns whether at least one complete interval was accepted.
		 * @return {@code true} if complete interval coverage is available; otherwise {@code false}
		 */
		boolean hasCompleteIntervals() {
			return !this.increments.isEmpty() && this.coveredStart != null && this.coveredEnd != null;
		}

		/**
		 * Returns whether accepted intervals do not cover both requested boundaries.
		 * @param request the requested calculation
		 * @return {@code true} if boundary coverage is incomplete; otherwise {@code false}
		 */
		boolean hasIncompleteBoundaryCoverage(CalculationRequest request) {
			Instant start = this.coveredStart;
			Instant end = this.coveredEnd;
			return start == null || end == null || !start.equals(request.from()) || !end.equals(request.to());
		}

		/**
		 * Returns whether any accepted or excluded interval makes the result partial.
		 * @return {@code true} if interval quality is partial; otherwise {@code false}
		 */
		boolean hasPartialIntervals() {
			return this.boundaryInterval || this.overlap || this.invalidAmount || this.outlier;
		}

		/**
		 * Returns the end of the latest accepted interval.
		 * @return the current coverage cursor
		 */
		Instant coverageCursor() {
			return this.coverageCursor;
		}

		/**
		 * Returns the accumulated precipitation amount.
		 * @return the total in native units
		 */
		BigDecimal total() {
			return this.total;
		}

		/**
		 * Returns the accepted rainfall increments.
		 * @return the interval increments
		 */
		List<RainfallIncrement> increments() {
			return this.increments;
		}

		/**
		 * Returns the start of accepted coverage.
		 * @return the coverage start, or {@code null} if no interval was accepted
		 */
		@Nullable Instant coveredStart() {
			return this.coveredStart;
		}

		/**
		 * Returns the end of accepted coverage.
		 * @return the coverage end, or {@code null} if no interval was accepted
		 */
		@Nullable Instant coveredEnd() {
			return this.coveredEnd;
		}

		/**
		 * Returns the first valid interval end in the requested range.
		 * @return the first valid observation time, or {@code null} if none was found
		 */
		@Nullable Instant firstValid() {
			return this.firstValid;
		}

		/**
		 * Returns the latest valid interval end in the requested range.
		 * @return the latest valid observation time, or {@code null} if none was found
		 */
		@Nullable Instant lastValid() {
			return this.lastValid;
		}

		/**
		 * Returns the largest uncovered period.
		 * @return the longest gap
		 */
		Duration longestGap() {
			return this.longestGap;
		}

		/**
		 * Returns the number of accepted intervals.
		 * @return the received sample count
		 */
		int received() {
			return this.received;
		}

	}

}
