package com.pembana.raingauge.rainfall;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.ObservationQuality;
import com.pembana.raingauge.observation.PrecipitationObservation;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class RainfallAccumulator {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final RainfallProperties properties;

	private final Clock clock;

	public RainfallAccumulator(RainfallProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public RainfallResult calculate(List<PrecipitationObservation> source, Instant from,
			Instant to, Duration cadence, ObservationBatch batch, RainfallUnit unit) {
		Instant calculatedAt = this.clock.instant();
		Deduplicated deduplicated = deduplicate(source);
		List<String> warnings = new ArrayList<>(batch.warnings());
		warnings.addAll(deduplicated.warnings());
		List<PrecipitationObservation> observations = deduplicated.observations();
		PrecipitationObservation baseline = findBaseline(observations, from);
		if (baseline == null) {
			warnings.add("No valid accumulator baseline was available at or before the requested start");
			return unavailable(from, to, calculatedAt, batch, unit, cadence, warnings,
					deduplicated.conflicts());
		}

		BigDecimal total = BigDecimal.ZERO;
		List<RainfallIncrement> increments = new ArrayList<>();
		PrecipitationObservation previous = baseline;
		Duration longestGap = Duration.ZERO;
		int unresolvedResets = 0;
		int recognizedResets = 0;
		boolean outlier = false;
		for (int index = 0; index < observations.size(); index++) {
			PrecipitationObservation current = observations.get(index);
			if (!current.validAt().isAfter(baseline.validAt()) || !current.validAt().isBefore(to)
					|| current.quality() != ObservationQuality.VALID) {
				continue;
			}
			Duration gap = Duration.between(previous.validAt(), current.validAt());
			if (gap.compareTo(longestGap) > 0 && current.validAt().isAfter(from)) {
				longestGap = gap;
			}
			BigDecimal delta = current.value().subtract(previous.value());
			String qualityFlag = null;
			if (delta.signum() < 0) {
				PrecipitationObservation next = nextValid(observations, index + 1, to);
				BigDecimal rollover = rolloverDelta(previous.value(), current.value());
				if (rollover != null) {
					delta = rollover;
					recognizedResets++;
					qualityFlag = "rollover";
					warnings.add("Accumulator rollover recognized at " + current.validAt());
				} else if (isCorroboratedReset(current, next, cadence)) {
					delta = current.value();
					recognizedResets++;
					qualityFlag = "reset";
					warnings.add("Accumulator reset recognized at " + current.validAt());
				} else {
					unresolvedResets++;
					warnings.add("Unresolved negative accumulator change at " + current.validAt());
					previous = current;
					continue;
				}
			}
			if (delta.compareTo(this.properties.getReset().getSuspectedOutlierIncrement()) > 0) {
				qualityFlag = "suspected-outlier";
				outlier = true;
				warnings.add("Unusually large positive increment at " + current.validAt());
			}
			if (current.validAt().isAfter(from)) {
				total = total.add(delta);
				increments.add(new RainfallIncrement(current.validAt(), delta, total, qualityFlag));
			}
			previous = current;
		}

		List<PrecipitationObservation> inWindow = observations.stream()
				.filter((observation) -> !observation.validAt().isBefore(from)
						&& observation.validAt().isBefore(to)
						&& observation.quality() == ObservationQuality.VALID)
				.toList();
		Instant first = inWindow.isEmpty() ? null : inWindow.getFirst().validAt();
		Instant last = inWindow.isEmpty() ? baseline.validAt() : inWindow.getLast().validAt();
		long expected = Math.max(1, Duration.between(from, to).toSeconds()
				/ Math.max(1, cadence.toSeconds()));
		long received = inWindow.size();
		BigDecimal completeness = BigDecimal.valueOf(Math.min(received, expected))
				.multiply(ONE_HUNDRED)
				.divide(BigDecimal.valueOf(expected), 1, RoundingMode.HALF_UP);
		boolean materialGap = longestGap.compareTo(cadence.multipliedBy(2)) > 0;
		if (materialGap) {
			warnings.add("Native observations contain a gap of " + longestGap.toMinutes() + " minutes");
		}
		Duration sourceAge = Duration.between(last, calculatedAt);
		boolean stale = sourceAge.compareTo(cadence.multipliedBy(2)) > 0;
		RainfallResultStatus status;
		if (deduplicated.conflicts() > 0) {
			status = RainfallResultStatus.CONFLICTING;
		} else if (unresolvedResets > 0 || materialGap || outlier) {
			status = RainfallResultStatus.PARTIAL;
		} else if (batch.staleCache() || stale) {
			status = RainfallResultStatus.STALE;
		} else {
			status = RainfallResultStatus.COMPLETE;
		}
		RainfallDataQuality quality = new RainfallDataQuality(expected, received, completeness,
				longestGap, first, last, unresolvedResets, recognizedResets, deduplicated.conflicts(),
				batch.warnings().size(), sourceAge, batch.staleCache());
		return new RainfallResult(new RainfallAmount(total), unit, "in",
				unit == RainfallUnit.IMPERIAL ? 2 : 1, new BigDecimal("0.01"), from, to,
				from, last, last, calculatedAt, status, warnings, quality, increments,
				batch.provider(), batch.fetchedAt());
	}

	private RainfallResult unavailable(Instant from, Instant to, Instant calculatedAt,
			ObservationBatch batch, RainfallUnit unit, Duration cadence, List<String> warnings,
			int conflicts) {
		long expected = Math.max(1, Duration.between(from, to).toSeconds()
				/ Math.max(1, cadence.toSeconds()));
		RainfallDataQuality quality = new RainfallDataQuality(expected, 0, BigDecimal.ZERO,
				Duration.ZERO, null, null, 0, 0, conflicts, batch.warnings().size(),
				Duration.ZERO, batch.staleCache());
		return new RainfallResult(null, unit, "in", unit == RainfallUnit.IMPERIAL ? 2 : 1,
				new BigDecimal("0.01"), from, to, null, null, null, calculatedAt,
				RainfallResultStatus.UNAVAILABLE, warnings, quality, List.of(), batch.provider(),
				batch.fetchedAt());
	}

	private Deduplicated deduplicate(List<PrecipitationObservation> source) {
		Map<Instant, List<PrecipitationObservation>> grouped = new TreeMap<>();
		for (PrecipitationObservation observation : source) {
			grouped.computeIfAbsent(observation.validAt(), (ignored) -> new ArrayList<>())
					.add(observation);
		}
		List<PrecipitationObservation> result = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		int conflicts = 0;
		for (Map.Entry<Instant, List<PrecipitationObservation>> entry : grouped.entrySet()) {
			Map<BigDecimal, List<PrecipitationObservation>> byValue = new LinkedHashMap<>();
			for (PrecipitationObservation observation : entry.getValue()) {
				byValue.computeIfAbsent(observation.value().stripTrailingZeros(),
						(ignored) -> new ArrayList<>()).add(observation);
			}
			if (byValue.size() > 1) {
				conflicts++;
				warnings.add("Conflicting retransmissions at " + entry.getKey());
			}
			PrecipitationObservation selected = entry.getValue().stream()
					.sorted(Comparator.comparingInt(this::qualityRank)
							.thenComparingInt(PrecipitationObservation::sourceOrder))
					.findFirst()
					.orElseThrow();
			result.add(selected);
		}
		return new Deduplicated(List.copyOf(result), List.copyOf(warnings), conflicts);
	}

	private int qualityRank(PrecipitationObservation observation) {
		return observation.quality() == ObservationQuality.VALID ? 0 : 1;
	}

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

	private @Nullable BigDecimal rolloverDelta(BigDecimal previous, BigDecimal current) {
		BigDecimal maximum = this.properties.getReset().getRolloverMaximum();
		BigDecimal rolloverFloor = maximum.multiply(new BigDecimal("0.95"));
		if (previous.compareTo(rolloverFloor) >= 0
				&& current.compareTo(this.properties.getReset().getNearZeroThreshold()) <= 0) {
			return maximum.subtract(previous).add(current);
		}
		return null;
	}

	private boolean isCorroboratedReset(PrecipitationObservation current,
			@Nullable PrecipitationObservation next, Duration cadence) {
		return current.value().compareTo(this.properties.getReset().getNearZeroThreshold()) <= 0
				&& next != null
				&& next.value().subtract(current.value()).signum() >= 0
				&& Duration.between(current.validAt(), next.validAt())
						.compareTo(cadence.multipliedBy(2)) <= 0;
	}

	private record Deduplicated(List<PrecipitationObservation> observations,
			List<String> warnings, int conflicts) {
	}

}
