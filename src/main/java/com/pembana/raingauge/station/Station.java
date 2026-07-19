package com.pembana.raingauge.station;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "weather_station", uniqueConstraints = @UniqueConstraint(
		name = "uk_weather_station_network_station_id", columnNames = { "network", "station_id" }))
public class Station {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 32)
	private String network;

	@Column(name = "station_id", nullable = false, length = 32)
	private String stationId;

	@Column(name = "source_name", nullable = false, length = 255)
	private String sourceName;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Column(length = 32)
	private @Nullable String alias;

	@Column(length = 64)
	private @Nullable String island;

	@Column(length = 128)
	private @Nullable String region;

	@Column(precision = 9, scale = 6)
	private @Nullable BigDecimal latitude;

	@Column(precision = 10, scale = 6)
	private @Nullable BigDecimal longitude;

	@Column(precision = 10, scale = 3)
	private @Nullable BigDecimal elevation;

	@Column(name = "archive_begin")
	private @Nullable LocalDate archiveBegin;

	@Column(name = "archive_end")
	private @Nullable LocalDate archiveEnd;

	@Column(name = "source_online", nullable = false)
	private boolean sourceOnline;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false)
	private boolean featured;

	@Column(name = "catalog_confirmed", nullable = false)
	private boolean catalogConfirmed = true;

	@Enumerated(EnumType.STRING)
	@Column(name = "rainfall_capability", nullable = false, length = 48)
	private RainfallCapability rainfallCapability = RainfallCapability.PRECIPITATION_TYPE_UNKNOWN;

	@Column(name = "precipitation_key", length = 16)
	private @Nullable String precipitationKey;

	@Column(length = 8)
	private @Nullable String state;

	@Column(length = 8)
	private @Nullable String country;

	@Column(name = "time_zone", length = 64)
	private @Nullable String timeZone;

	@Column(name = "disabled_reason", length = 255)
	private @Nullable String disabledReason;

	@Column(name = "override_note", length = 500)
	private @Nullable String overrideNote;

	@Column(name = "source_metadata", length = 4000)
	private @Nullable String sourceMetadata;

	@Column(name = "latest_observation_at")
	private @Nullable Instant latestObservationAt;

	@Column(name = "catalog_last_seen_at")
	private @Nullable Instant catalogLastSeenAt;

	@Column(name = "catalog_refreshed_at")
	private @Nullable Instant catalogRefreshedAt;

	protected Station() {
	}

	public Station(String network, String stationId, String sourceName) {
		this.network = network;
		this.stationId = stationId;
		this.sourceName = sourceName;
		this.displayName = sourceName;
	}

	public void updateSourceMetadata(String sourceName, @Nullable BigDecimal latitude,
			@Nullable BigDecimal longitude, @Nullable BigDecimal elevation, boolean sourceOnline,
			@Nullable LocalDate archiveBegin, @Nullable LocalDate archiveEnd, @Nullable String state,
			@Nullable String country, @Nullable String timeZone, @Nullable String sourceMetadata,
			Instant refreshedAt) {
		this.sourceName = sourceName;
		this.displayName = sourceName;
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		this.sourceOnline = sourceOnline;
		this.archiveBegin = archiveBegin;
		this.archiveEnd = archiveEnd;
		this.state = state;
		this.country = country;
		this.timeZone = timeZone;
		this.sourceMetadata = sourceMetadata;
		this.catalogConfirmed = true;
		this.catalogLastSeenAt = refreshedAt;
		this.catalogRefreshedAt = refreshedAt;
	}

	public void applyOverride(StationOverride override) {
		if (override.alias() != null) {
			this.alias = override.alias();
		}
		if (override.preferredName() != null) {
			this.displayName = override.preferredName();
		}
		if (override.island() != null) {
			this.island = override.island();
		}
		if (override.region() != null) {
			this.region = override.region();
		}
		if (override.enabled() != null) {
			this.enabled = override.enabled();
		}
		if (override.featured() != null) {
			this.featured = override.featured();
		}
		if (override.disabledReason() != null) {
			this.disabledReason = override.disabledReason();
		}
		if (override.precipitationKey() != null) {
			this.precipitationKey = override.precipitationKey();
			this.rainfallCapability = override.precipitationKey().startsWith("PC")
					? RainfallCapability.SUPPORTED_ACCUMULATOR
					: RainfallCapability.PRECIPITATION_TYPE_UNKNOWN;
		}
		if (override.note() != null) {
			this.overrideNote = override.note();
		}
	}

	public void updateCapability(RainfallCapability capability, @Nullable String precipitationKey) {
		this.rainfallCapability = capability;
		this.precipitationKey = precipitationKey;
	}

	public void markNotSeenDuringRefresh(Instant refreshedAt) {
		this.catalogConfirmed = false;
		this.catalogRefreshedAt = refreshedAt;
	}

	public void recordLatestObservation(Instant latestObservationAt) {
		if (this.latestObservationAt == null || latestObservationAt.isAfter(this.latestObservationAt)) {
			this.latestObservationAt = latestObservationAt;
		}
	}

	public UUID getId() {
		return this.id;
	}

	public String getNetwork() {
		return this.network;
	}

	public String getStationId() {
		return this.stationId;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public @Nullable String getAlias() {
		return this.alias;
	}

	public @Nullable String getIsland() {
		return this.island;
	}

	public @Nullable String getRegion() {
		return this.region;
	}

	public @Nullable BigDecimal getLatitude() {
		return this.latitude;
	}

	public @Nullable BigDecimal getLongitude() {
		return this.longitude;
	}

	public @Nullable BigDecimal getElevation() {
		return this.elevation;
	}

	public @Nullable LocalDate getArchiveBegin() {
		return this.archiveBegin;
	}

	public @Nullable LocalDate getArchiveEnd() {
		return this.archiveEnd;
	}

	public boolean isSourceOnline() {
		return this.sourceOnline;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isFeatured() {
		return this.featured;
	}

	public boolean isCatalogConfirmed() {
		return this.catalogConfirmed;
	}

	public RainfallCapability getRainfallCapability() {
		return this.rainfallCapability;
	}

	public @Nullable String getPrecipitationKey() {
		return this.precipitationKey;
	}

	public @Nullable String getState() {
		return this.state;
	}

	public @Nullable String getCountry() {
		return this.country;
	}

	public @Nullable String getTimeZone() {
		return this.timeZone;
	}

	public @Nullable String getDisabledReason() {
		return this.disabledReason;
	}

	public @Nullable String getOverrideNote() {
		return this.overrideNote;
	}

	public @Nullable String getSourceMetadata() {
		return this.sourceMetadata;
	}

	public @Nullable Instant getLatestObservationAt() {
		return this.latestObservationAt;
	}

	public @Nullable Instant getCatalogLastSeenAt() {
		return this.catalogLastSeenAt;
	}

	public @Nullable Instant getCatalogRefreshedAt() {
		return this.catalogRefreshedAt;
	}

}
