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

package com.pembana.raingauge.observation.shef;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests SHEF precipitation code interpretation.
 * @author Gunnar Hillert
 */
class ShefPrecipitationCodeTests {

	/**
	 * Verifies fixed minute, hour, day, and week durations.
	 */
	@Test
	void resolvesFixedIntervalDurations() {
		assertThat(ShefPrecipitationCode.fixedInterval("PPCRGZZ"))
				.contains(Duration.ofMinutes(15));
		assertThat(ShefPrecipitationCode.fixedInterval("PPHRG"))
				.contains(Duration.ofHours(1));
		assertThat(ShefPrecipitationCode.fixedInterval("PPDRZ"))
				.contains(Duration.ofDays(1));
		assertThat(ShefPrecipitationCode.fixedInterval("PPWRZ"))
				.contains(Duration.ofDays(7));
	}

	/**
	 * Verifies the one-day SHEF default for interval precipitation.
	 */
	@Test
	void resolvesDefaultIntervalDuration() {
		assertThat(ShefPrecipitationCode.fixedInterval("PP"))
				.contains(Duration.ofDays(1));
		assertThat(ShefPrecipitationCode.fixedInterval("PPZRZ"))
				.contains(Duration.ofDays(1));
	}

	/**
	 * Verifies variable and calendar-dependent durations remain unsupported.
	 */
	@Test
	void rejectsNonFixedAndNonIntervalCodes() {
		assertThat(ShefPrecipitationCode.fixedInterval("PPVRZ")).isEmpty();
		assertThat(ShefPrecipitationCode.fixedInterval("PPPRZ")).isEmpty();
		assertThat(ShefPrecipitationCode.fixedInterval("PPMRZ")).isEmpty();
		assertThat(ShefPrecipitationCode.fixedInterval("PCIRG")).isEmpty();
	}

}
