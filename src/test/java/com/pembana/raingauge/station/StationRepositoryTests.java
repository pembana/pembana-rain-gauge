package com.pembana.raingauge.station;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "hawaii.rainfall.catalog.startup-enabled=false")
class StationRepositoryTests {

	@Autowired
	private StationRepository repository;

	@BeforeEach
	void clearDatabase() {
		this.repository.deleteAll();
	}

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
	}

	@Test
	void disabledStationIsExcludedFromPublicSelectorQuery() {
		Station station = new Station("HI_DCP", "OFFH1", "Disabled station");
		station.applyOverride(new StationOverride(null, null, null, null, false, null,
				"manual review", null, null));
		this.repository.saveAndFlush(station);

		assertThat(this.repository.findAllByEnabledTrueOrderByDisplayNameAsc()).isEmpty();
		assertThat(this.repository.findByStationIdIgnoreCase("offh1")).isPresent();
	}

}
