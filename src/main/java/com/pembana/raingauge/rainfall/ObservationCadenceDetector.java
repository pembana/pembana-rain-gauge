package com.pembana.raingauge.rainfall;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pembana.raingauge.observation.ObservationQuality;
import com.pembana.raingauge.observation.PrecipitationObservation;

import org.springframework.stereotype.Component;

@Component
public class ObservationCadenceDetector {

	private static final Duration DEFAULT_CADENCE = Duration.ofMinutes(15);

	public Duration detect(List<PrecipitationObservation> observations) {
		List<Instant> timestamps = observations.stream()
				.filter((observation) -> observation.quality() == ObservationQuality.VALID)
				.map(PrecipitationObservation::validAt)
				.distinct()
				.sorted()
				.toList();
		if (timestamps.size() < 2) {
			return DEFAULT_CADENCE;
		}
		List<Long> intervals = new ArrayList<>();
		for (int index = 1; index < timestamps.size(); index++) {
			long seconds = Duration.between(timestamps.get(index - 1), timestamps.get(index)).toSeconds();
			if (seconds > 0 && seconds <= Duration.ofHours(6).toSeconds()) {
				intervals.add(seconds);
			}
		}
		if (intervals.isEmpty()) {
			return DEFAULT_CADENCE;
		}
		Map<Long, Long> frequencies = new HashMap<>();
		for (Long interval : intervals) {
			frequencies.merge(interval, 1L, Long::sum);
		}
		long mode = frequencies.entrySet().stream()
				.max(Comparator.<Map.Entry<Long, Long>>comparingLong(Map.Entry::getValue)
						.thenComparingLong((entry) -> -entry.getKey()))
				.map(Map.Entry::getKey)
				.orElse(DEFAULT_CADENCE.toSeconds());
		return Duration.ofSeconds(mode);
	}

}
