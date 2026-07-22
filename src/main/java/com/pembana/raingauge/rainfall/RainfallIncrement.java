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

package com.pembana.raingauge.rainfall;

import java.math.BigDecimal;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Describes a rainfall increment.
 * @param at the time at which the provider operation completed
 * @param inches the inches
 * @param cumulativeInches the cumulative inches
 * @param qualityFlag the quality flag
 * @author Gunnar Hillert
 */
public record RainfallIncrement(
		Instant at,
		BigDecimal inches,
		BigDecimal cumulativeInches,
		@Nullable String qualityFlag) {
}
