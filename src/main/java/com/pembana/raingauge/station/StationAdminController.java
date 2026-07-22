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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles station admin HTTP requests.
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/admin/station-catalog")
public class StationAdminController {

	private final StationService stationService;

	/**
	 * Creates a new {@code StationAdminController}.
	 * @param stationService the station service
	 */
	public StationAdminController(StationService stationService) {
		this.stationService = stationService;
	}

	/**
	 * Refreshes the station catalog immediately.
	 * @return the catalog-refresh summary response
	 */
	@PostMapping("/refresh")
	public ResponseEntity<StationService.CatalogRefreshSummary> refresh() {
		return ResponseEntity.ok(this.stationService.refreshCatalog());
	}

}
