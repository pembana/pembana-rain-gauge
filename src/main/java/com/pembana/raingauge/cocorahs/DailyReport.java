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

package com.pembana.raingauge.cocorahs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * A CoCoRaHS report with its original observation window and revision timestamp.
 *
 * @param start first report date covered
 * @param observedAt gauge reading in Hawaiʻi local time
 * @param revisedAt provider revision timestamp
 * @param inches reported measurable precipitation, if present
 * @param trace whether the observer reported a trace
 * @param multiDay whether this is a multi-day accumulation
 * @author OpenAI Codex
 */
public record DailyReport(LocalDate start, LocalDateTime observedAt, LocalDateTime revisedAt,
		@Nullable BigDecimal inches, boolean trace, boolean multiDay) {

	public LocalDate end() {
		return this.observedAt.toLocalDate();
	}

	boolean usable() {
		return this.inches != null || this.trace;
	}

}
