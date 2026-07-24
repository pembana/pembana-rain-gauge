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

		BigDecimal total = BigDecimal.ZERO;
		List<RainfallIncrement> increments = new ArrayList<>();
		Instant coverageCursor = from;
		Instant coveredStart = null;
		Instant coveredEnd = null;
		Instant firstValid = null;
		Instant lastValid = null;
		Duration longestGap = Duration.ZERO;
		boolean boundaryInterval = false;
		boolean overlap = false;
		boolean invalidAmount = false;
		boolean outlier = false;
		int received = 0;

		for (PrecipitationObservation observation : deduplicated.observations()) {
			if (observation.quality() != ObservationQuality.VALID) {
				continue;
			}
			Instant intervalEnd = observation.validAt();
			Instant intervalStart = intervalEnd.minus(interval);
			if (!intervalEnd.isAfter(from) || intervalEnd.isAfter(to)) {
				continue;
			}
			if (firstValid == null) {
				firstValid = intervalEnd;
			}
			lastValid = intervalEnd;
			if (intervalStart.isBefore(from)) {
				boundaryInterval = true;
				continue;
			}
			if (observation.value().signum() < 0) {
				invalidAmount = true;
				warnings.add("Negative interval precipitation was excluded at " + intervalEnd);
				continue;
			}
			if (intervalStart.isBefore(coverageCursor)) {
				overlap = true;
				warnings.add("Overlapping precipitation interval was excluded at " + intervalEnd);
				continue;
			}
			Duration gap = Duration.between(coverageCursor, intervalStart);
			if (gap.compareTo(longestGap) > 0) {
				longestGap = gap;
			}
			String qualityFlag = null;
			if (observation.value()
					.compareTo(this.properties.getReset().getSuspectedOutlierIncrement()) > 0) {
				qualityFlag = "suspected-outlier";
				outlier = true;
				warnings.add("Unusually large interval precipitation at " + intervalEnd);
			}
			total = total.add(observation.value());
			increments.add(new RainfallIncrement(intervalEnd, observation.value(), total,
					qualityFlag));
			received++;
			if (coveredStart == null) {
				coveredStart = intervalStart;
			}
			coveredEnd = intervalEnd;
			coverageCursor = intervalEnd;
		}

		if (boundaryInterval) {
			warnings.add("An interval crossing the requested start was excluded because it "
					+ "cannot be divided without estimating rainfall");
		}
		if (increments.isEmpty()) {
			warnings.add("No complete interval precipitation observations were available in "
					+ "the requested range");
			return unavailable(from, to, calculatedAt, batch, unit, interval, warnings,
					deduplicated.conflicts());
		}
		boolean incompleteBoundaryCoverage = !coveredStart.equals(from) || !coveredEnd.equals(to);
		if (incompleteBoundaryCoverage) {
			warnings.add("Requested range is not fully covered by complete precipitation "
					+ "intervals");
		}

		Duration trailingGap = Duration.between(coverageCursor, to);
		if (trailingGap.compareTo(longestGap) > 0) {
			longestGap = trailingGap;
		}
		long expected = Math.max(1, Duration.between(from, to).toSeconds()
				/ Math.max(1, interval.toSeconds()));
		BigDecimal completeness = BigDecimal.valueOf(Math.min(received, expected))
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(expected), 1, RoundingMode.HALF_UP);
		boolean materialGap = longestGap.compareTo(interval) >= 0;
		if (materialGap) {
			warnings.add("Interval observations leave an uncovered gap of "
					+ longestGap.toMinutes() + " minutes");
		}
		Duration sourceAge = Duration.between(lastValid, calculatedAt);
		boolean stale = sourceAge.compareTo(interval.multipliedBy(2)) > 0;
		RainfallResultStatus status;
		if (deduplicated.conflicts() > 0) {
			status = RainfallResultStatus.CONFLICTING;
		}
		else if (boundaryInterval || incompleteBoundaryCoverage || overlap || invalidAmount
				|| outlier || materialGap || received < expected) {
			status = RainfallResultStatus.PARTIAL;
		}
		else if (batch.staleCache() || stale) {
			status = RainfallResultStatus.STALE;
		}
		else {
			status = RainfallResultStatus.COMPLETE;
		}
		RainfallDataQuality quality = new RainfallDataQuality(expected, received, completeness,
				longestGap, firstValid, lastValid, 0, 0, deduplicated.conflicts(),
				batch.warnings().size(), sourceAge, batch.staleCache());
		return new RainfallResult(new RainfallAmount(total), RainfallMethod.INTERVAL, unit, "in",
				(unit == RainfallUnit.IMPERIAL) ? 2 : 1, new BigDecimal("0.01"), from, to,
				coveredStart, coveredEnd, lastValid, calculatedAt, status, warnings, quality,
				increments, batch.provider(), batch.fetchedAt());
	}

	/**
	 * Creates an unavailable interval rainfall result with quality metadata.
	 * @param from the inclusive start of the requested range
	 * @param to the exclusive end of the requested range
	 * @param calculatedAt the calculation time
	 * @param batch the source observation batch
	 * @param unit the requested rainfall unit
	 * @param interval the observation interval
	 * @param warnings the result warnings
	 * @param conflicts the number of conflicting timestamps
	 * @return the unavailable rainfall result
	 */
	private RainfallResult unavailable(Instant from, Instant to, Instant calculatedAt,
			ObservationBatch batch, RainfallUnit unit, Duration interval, List<String> warnings,
			int conflicts) {
		long expected = Math.max(1, Duration.between(from, to).toSeconds()
				/ Math.max(1, interval.toSeconds()));
		RainfallDataQuality quality = new RainfallDataQuality(expected, 0, BigDecimal.ZERO,
				Duration.ZERO, null, null, 0, 0, conflicts, batch.warnings().size(),
				Duration.ZERO, batch.staleCache());
		return new RainfallResult(null, RainfallMethod.INTERVAL, unit, "in",
				(unit == RainfallUnit.IMPERIAL) ? 2 : 1, new BigDecimal("0.01"), from, to,
				null, null, null, calculatedAt, RainfallResultStatus.UNAVAILABLE, warnings,
				quality, List.of(), batch.provider(), batch.fetchedAt());
	}

}
