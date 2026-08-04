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

package com.pembana.raingauge.observation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests HADS observation parser.
 * @author Gunnar Hillert
 */
class HadsObservationParserTests {

	private final HadsObservationParser parser = new HadsObservationParser();

	/**
	 * Verifies that parses representative wide response.
	 * @throws IOException if an I/O operation fails
	 */
	@Test
	void parsesRepresentativeWideResponse() throws IOException {
		ObservationParseResult result = this.parser.parse(fixture("iem-wihh1-hads.csv"), "PCIRG");

		assertThat(result.observations()).hasSize(6);
		assertThat(result.rejectedRows()).isZero();
		assertThat(result.observations().get(2).stationId()).isEqualTo("WIHH1");
		assertThat(result.observations().get(2).sourceKey()).isEqualTo("PCIRGZZ");
		assertThat(result.observations().get(2).shefKey()).isEqualTo("PCIRG");
		assertThat(result.observations().get(2).validAt())
				.isEqualTo(Instant.parse("2026-07-01T00:30:00Z"));
		assertThat(result.observations().get(2).value()).isEqualByComparingTo("42.01");
	}

	/**
	 * Verifies that interval precipitation is selected from a representative wide
	 * response.
	 * @throws IOException if an I/O operation fails
	 */
	@Test
	void parsesIntervalPrecipitationFromWideResponse() throws IOException {
		ObservationParseResult result = this.parser.parse(fixture("iem-wihh1-hads.csv"),
				"PPHRG");

		assertThat(result.observations()).hasSize(2);
		assertThat(result.observations()).extracting(PrecipitationObservation::shefKey)
				.containsOnly("PPHRG");
		assertThat(result.observations()).extracting(PrecipitationObservation::value)
				.containsExactly(new java.math.BigDecimal("0.0"),
						new java.math.BigDecimal("0.03"));
	}

	/**
	 * Verifies that validates header names rather than positions.
	 * @throws IOException if an I/O operation fails
	 */
	@Test
	void validatesHeaderNamesRatherThanPositions() throws IOException {
		ObservationParseResult result = this.parser.parse(
				fixture("iem-wihh1-hads-reordered.csv"), "PCIRG");

		assertThat(result.observations()).hasSize(3);
		assertThat(result.rejectedRows()).isEqualTo(2);
		assertThat(result.warnings()).allMatch((warning) -> warning.contains("rejected"));
		assertThat(result.observations()).extracting(PrecipitationObservation::value)
				.containsExactly(new java.math.BigDecimal("41.99"), new java.math.BigDecimal("42.01"),
						new java.math.BigDecimal("42.03"));
	}

	/**
	 * Verifies that rejects response without required columns.
	 */
	@Test
	void rejectsResponseWithoutRequiredColumns() {
		ObservationParseResult result = this.parser.parse("time,value\nnow,1.0", "PCIRG");

		assertThat(result.observations()).isEmpty();
		assertThat(result.rejectedRows()).isEqualTo(1);
		assertThat(result.warnings()).containsExactly(
				"Required station and utc_valid columns were not present");
	}

	/**
	 * Verifies that reports empty response.
	 */
	@Test
	void reportsEmptyResponse() {
		ObservationParseResult result = this.parser.parse("", "PCIRG");

		assertThat(result.observations()).isEmpty();
		assertThat(result.warnings()).containsExactly("Provider response was empty");
	}

	/**
	 * Verifies that preserves qualifier quality when supplied.
	 */
	@Test
	void preservesQualifierQualityWhenSupplied() {
		String response = """
				station,utc_valid,PCIRG,qualifier
				WIHH1,2026-07-01 00:00:00,1.00,A
				WIHH1,2026-07-01 00:15:00,1.01,invalid""";

		ObservationParseResult result = this.parser.parse(response, "PCIRG");

		assertThat(result.observations()).extracting(PrecipitationObservation::quality)
				.containsExactly(ObservationQuality.SUSPECT, ObservationQuality.MALFORMED_QUALIFIER);
	}

	/**
	 * Loads a test fixture from the classpath.
	 * @param name the name
	 * @return the resulting fixture
	 * @throws IOException if an I/O operation fails
	 */
	private String fixture(String name) throws IOException {
		return new ClassPathResource("fixtures/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

}
