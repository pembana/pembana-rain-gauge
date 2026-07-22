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

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests station repository.
 * @author Gunnar Hillert
 */
@SpringBootTest(properties = "hawaii.rainfall.catalog.startup-enabled=false")
class StationRepositoryTests {

	@Autowired
	private StationRepository repository;

	/**
	 * Clears persisted stations before each repository test.
	 */
	@BeforeEach
	void clearDatabase() {
		this.repository.deleteAll();
	}

	/**
	 * Verifies that flyway creates schema and repository persists entity on H2.
	 */
	@Test
	void flywayCreatesSchemaAndRepositoryPersistsEntityOnH2() {
		Station station = new Station("HI_DCP", "WIHH1", "Kailua-Kona 3SE - Waiaha");
		station.updateSourceMetadata("Kailua-Kona 3SE - Waiaha", null, null, null, true,
				null, null, "HI", "US", "Pacific/Honolulu", "fixture", Instant.now());
		station.applyOverride(new StationOverride("HI82", "Waiaha", "Hawaiʻi", "North Kona",
				true, true, null, "PCIRG", null));

		this.repository.saveAndFlush(station);

		Station loaded = this.repository.findByNetworkAndStationId("HI_DCP", "WIHH1").orElseThrow();
		assertThat(loaded.getDisplayName()).isEqualTo("Waiaha");
		assertThat(loaded.getSourceName()).isEqualTo("Kailua-Kona 3SE - Waiaha");
		assertThat(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc())
				.singleElement()
				.extracting(Station::getId)
				.isEqualTo(loaded.getId());
		assertThat(this.repository.findRainfallStations(
				RainfallCapability.SUPPORTED_ACCUMULATOR))
				.singleElement()
				.extracting(Station::getStationId)
				.isEqualTo("WIHH1");
	}

	/**
	 * Verifies that disabled station is excluded from public selector query.
	 */
	@Test
	void disabledStationIsExcludedFromPublicSelectorQuery() {
		Station station = new Station("HI_DCP", "OFFH1", "Disabled station");
		station.applyOverride(new StationOverride(null, null, null, null, false, null,
				"manual review", null, null));
		this.repository.saveAndFlush(station);

		assertThat(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc()).isEmpty();
		assertThat(this.repository.findByStationIdIgnoreCase("offh1")).isPresent();
	}

	/**
	 * Verifies that unsupported rainfall station is excluded from rainfall selector query.
	 */
	@Test
	void unsupportedRainfallStationIsExcludedFromRainfallSelectorQuery() {
		Station station = new Station("HI_DCP", "HLRH1", "Interval-only station");
		station.updateCapability(RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION, "PPHRZ");
		this.repository.saveAndFlush(station);

		assertThat(this.repository.findRainfallStations(
				RainfallCapability.SUPPORTED_ACCUMULATOR))
				.isEmpty();
		assertThat(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc())
				.singleElement()
				.extracting(Station::getStationId)
				.isEqualTo("HLRH1");
	}

}
