package com.pembana.raingauge.observation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record ObservationBatch(
		List<PrecipitationObservation> observations,
		List<String> warnings,
		Instant fetchedAt,
		Duration cacheAge,
		boolean fromCache,
		boolean staleCache,
		String provider,
		int rejectedRows) {

	public ObservationBatch {
		observations = List.copyOf(observations);
		warnings = List.copyOf(warnings);
	}

	public ObservationBatch asCached(Instant now, boolean stale, String warning) {
		List<String> updatedWarnings = new ArrayList<>(this.warnings);
		if (!warning.isBlank()) {
			updatedWarnings.add(warning);
		}
		return new ObservationBatch(this.observations, updatedWarnings, this.fetchedAt,
				Duration.between(this.fetchedAt, now), true, stale, this.provider, this.rejectedRows);
	}
}
