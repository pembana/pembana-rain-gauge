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

package com.pembana.raingauge.station;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles station API HTTP requests.
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/api/stations")
public class StationApiController {

	private final StationService stationService;

	/**
	 * Creates a new {@code StationApiController}.
	 * @param stationService the station service
	 */
	public StationApiController(StationService stationService) {
		this.stationService = stationService;
	}

	/**
	 * Returns the public station catalog.
	 * @return the resulting stations
	 */
	@GetMapping
	public List<StationResponse> stations() {
		return this.stationService.findRainfallStations().stream().map(StationResponse::from).toList();
	}

	/**
	 * Creates a station for a test scenario.
	 * @param stationId the provider station identifier
	 * @return the resulting station
	 */
	@GetMapping("/{stationId}")
	public StationResponse station(@PathVariable String stationId) {
		return StationResponse.from(this.stationService.requirePublicStation(stationId));
	}

}
