package com.pembana.raingauge.observation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class HadsObservationParserTests {

	private final HadsObservationParser parser = new HadsObservationParser();

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

	@Test
	void rejectsResponseWithoutRequiredColumns() {
		ObservationParseResult result = this.parser.parse("time,value\nnow,1.0", "PCIRG");

		assertThat(result.observations()).isEmpty();
		assertThat(result.rejectedRows()).isEqualTo(1);
		assertThat(result.warnings()).containsExactly(
				"Required station and utc_valid columns were not present");
	}

	@Test
	void reportsEmptyResponse() {
		ObservationParseResult result = this.parser.parse("", "PCIRG");

		assertThat(result.observations()).isEmpty();
		assertThat(result.warnings()).containsExactly("Provider response was empty");
	}

	@Test
	void preservesQualifierQualityWhenSupplied() {
		String response = "station,utc_valid,PCIRG,qualifier\n"
				+ "WIHH1,2026-07-01 00:00:00,1.00,A\n"
				+ "WIHH1,2026-07-01 00:15:00,1.01,invalid";

		ObservationParseResult result = this.parser.parse(response, "PCIRG");

		assertThat(result.observations()).extracting(PrecipitationObservation::quality)
				.containsExactly(ObservationQuality.SUSPECT, ObservationQuality.MALFORMED_QUALIFIER);
	}

	private String fixture(String name) throws IOException {
		return new ClassPathResource("fixtures/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

}
