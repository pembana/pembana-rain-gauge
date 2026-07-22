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

import org.jspecify.annotations.Nullable;

/**
 * Describes a station override.
 * @param alias the alias
 * @param preferredName the preferred name
 * @param island the island
 * @param region the region
 * @param enabled the enabled
 * @param featured the featured
 * @param disabledReason the disabled reason
 * @param precipitationKey the precipitation key
 * @param note the note
 * @author Gunnar Hillert
 */
public record StationOverride(
		@Nullable String alias,
		@Nullable String preferredName,
		@Nullable String island,
		@Nullable String region,
		@Nullable Boolean enabled,
		@Nullable Boolean featured,
		@Nullable String disabledReason,
		@Nullable String precipitationKey,
		@Nullable String note) {
}
