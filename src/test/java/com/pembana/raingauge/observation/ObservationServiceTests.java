package com.pembana.raingauge.observation;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.client.HadsObservationClient;
import com.pembana.raingauge.station.client.ProviderException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObservationServiceTests {

	private static final Instant FROM = Instant.parse("2026-07-01T00:00:00Z");

	private static final Instant TO = Instant.parse("2026-07-01T01:00:00Z");

	@Test
	void fallsBackToAStaleSuccessfulResponseWhenTheProviderFails() throws Exception {
		HadsObservationClient client = mock(HadsObservationClient.class);
		ObservationBatch successful = new ObservationBatch(List.of(
				PrecipitationObservation.valid("WIHH1", FROM, "PCIRG", new BigDecimal("1.00"), 0),
				PrecipitationObservation.valid("WIHH1", TO, "PCIRG", new BigDecimal("1.02"), 1)),
				List.of(), TO, Duration.ZERO, false, false, "fixture", 0);
		when(client.fetch(List.of("WIHH1"), "HI_DCP", "PCIRG", FROM, TO))
				.thenReturn(successful)
				.thenThrow(new ProviderException("provider unavailable"));
		RainfallProperties properties = new RainfallProperties();
		properties.getCache().setObservations(Duration.ofMillis(5));
		properties.getCache().setStaleObservations(Duration.ofMinutes(1));
		ObservationService service = new ObservationService(client, properties,
				Clock.fixed(TO, ZoneOffset.UTC));

		assertThat(service.observations("WIHH1", "HI_DCP", "PCIRG", FROM, TO).staleCache())
				.isFalse();
		Thread.sleep(20);
		ObservationBatch fallback = service.observations("WIHH1", "HI_DCP", "PCIRG", FROM, TO);

		assertThat(fallback.staleCache()).isTrue();
		assertThat(fallback.warnings()).contains(
				"Live provider request failed; a stale cached response is shown");
		assertThat(fallback.observations()).hasSize(2);
	}

}
