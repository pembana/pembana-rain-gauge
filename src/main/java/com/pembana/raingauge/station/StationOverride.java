package com.pembana.raingauge.station;

import org.jspecify.annotations.Nullable;

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
