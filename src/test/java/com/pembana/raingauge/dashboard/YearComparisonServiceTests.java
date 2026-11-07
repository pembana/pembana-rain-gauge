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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.observation.PrecipitationObservation;
import com.pembana.raingauge.rainfall.RainfallService;
import com.pembana.raingauge.rainfall.RainfallUnit;
import com.pembana.raingauge.rainfall.interval.IntervalRainfallCalculator;
import com.pembana.raingauge.station.RainfallCapability;
import com.pembana.raingauge.station.Station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class YearComparisonServiceTests {

	@Test
	void matchingPeriodsClampLeapDayAndReuseTheSameCutoff() {
		Instant now = Instant.parse("2024-02-29T22:07:31Z");
		Instant historicalStart = Instant.parse("2023-01-01T10:00:00Z");
		Instant historicalCutoff = Instant.parse("2023-02-28T22:05:00Z");
		Instant leapStart = Instant.parse("2024-01-01T10:00:00Z");
		Instant leapCutoff = Instant.parse("2024-02-29T22:05:00Z");
		RainfallProperties properties = new RainfallProperties();
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		RainfallService rainfall = mock(RainfallService.class);
		YearComparisonService service = new YearComparisonService(rainfall, properties, clock);
		Station station = new Station("HI_DCP", "TESTH1", "Test station");
		station.updateCapability(RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION, "PPHRG");
		RainfallUnit unit = RainfallUnit.IMPERIAL;
		IntervalRainfallCalculator calculator = new IntervalRainfallCalculator(properties, clock);
		List<PrecipitationObservation> source = List.of(
				PrecipitationObservation.valid("TESTH1", Instant.parse("2023-02-01T10:00:00Z"),
						"PPHRG", new BigDecimal("0.20"), 0),
				PrecipitationObservation.valid("TESTH1", Instant.parse("2023-02-01T11:00:00Z"),
						"PPHRG", BigDecimal.ZERO, 1));
		given(rainfall.calculate(eq(station), any(Instant.class), any(Instant.class), eq(unit)))
			.willAnswer((call) -> calculator.calculate(source, call.getArgument(1),
					call.getArgument(2), Duration.ofHours(1),
					new ObservationBatch(source, List.of(), now, Duration.ZERO, false, false,
							"fixture", 0), unit));

		YearComparisonResponse response = service.compare(station, List.of(2024, 2023), "ytd", unit);
		YearComparisonResponse.YearResult common = response.years().getFirst();
		assertThat(common.to()).isEqualTo(LocalDateTime.parse("2023-02-28T12:05"));
		assertThat(common.months().get(0).value()).isEqualByComparingTo("0.20");
		assertThat(common.months().get(1).value()).isEqualByComparingTo("0.00");
		assertThat(common.months().get(2).value()).isNull();
		assertThat(response.years().getLast().to()).isEqualTo(LocalDateTime.parse("2024-02-29T12:05"));

		assertThat(service.compare(station, List.of(2023, 2024), "ytd", unit)).isEqualTo(response);
		verify(rainfall, times(1)).calculate(station, historicalStart, historicalCutoff, unit);
		verify(rainfall, times(1)).calculate(station, leapStart, leapCutoff, unit);
	}

}
