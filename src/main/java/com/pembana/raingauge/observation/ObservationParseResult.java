package com.pembana.raingauge.observation;

import java.util.List;

public record ObservationParseResult(
		List<PrecipitationObservation> observations,
		List<String> warnings,
		int parsedRows,
		int rejectedRows) {

	public ObservationParseResult {
		observations = List.copyOf(observations);
		warnings = List.copyOf(warnings);
	}
}
