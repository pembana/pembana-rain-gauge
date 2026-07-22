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

package com.pembana.raingauge.dashboard;

import java.util.List;

import com.pembana.raingauge.station.StationResponse;

import org.jspecify.annotations.Nullable;

/**
 * Describes a comparison view.
 * @param availableStations the available stations
 * @param selectedStationIds the selected station ids
 * @param period the period
 * @param unit the requested rainfall unit
 * @param comparison the comparison
 * @param error the error
 * @author Gunnar Hillert
 */
public record ComparisonView(
		List<StationResponse> availableStations,
		List<String> selectedStationIds,
		String period,
		String unit,
		@Nullable ComparisonResponse comparison,
		@Nullable String error) {

	/**
	 * Creates a new {@code ComparisonView}.
	 * @param availableStations the available stations
	 * @param selectedStationIds the selected station ids
	 * @param period the period
	 * @param unit the requested rainfall unit
	 * @param comparison the comparison
	 * @param error the error
	 */
	public ComparisonView {
		availableStations = List.copyOf(availableStations);
		selectedStationIds = List.copyOf(selectedStationIds);
	}
}
