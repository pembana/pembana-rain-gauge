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

import com.pembana.raingauge.observation.shef.ShefPrecipitationCode;

/**
 * Represents a rainfall monitoring station and its provider metadata.
 * @author Gunnar Hillert
 */
@Entity
@Table(name = "weather_station", uniqueConstraints = @UniqueConstraint(
		name = "uk_weather_station_network_station_id", columnNames = { "network", "station_id" }))
public class Station {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private @Nullable UUID id;

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

	/**
	 * Creates an empty station for persistence frameworks.
	 */
	protected Station() {
	}

	/**
	 * Creates a new {@code Station}.
	 * @param network the provider network identifier
	 * @param stationId the provider station identifier
	 * @param sourceName the source name
	 */
	public Station(String network, String stationId, String sourceName) {
		this.network = network;
		this.stationId = stationId;
		this.sourceName = sourceName;
		this.displayName = sourceName;
	}

	/**
	 * Updates source metadata.
	 * @param metadata the source metadata
	 * @param refreshedAt the refreshed at
	 */
	public void updateSourceMetadata(SourceMetadata metadata, Instant refreshedAt) {
		this.sourceName = metadata.sourceName();
		this.displayName = metadata.sourceName();
		this.latitude = metadata.latitude();
		this.longitude = metadata.longitude();
		this.elevation = metadata.elevation();
		this.sourceOnline = metadata.sourceOnline();
		this.archiveBegin = metadata.archiveBegin();
		this.archiveEnd = metadata.archiveEnd();
		this.state = metadata.state();
		this.country = metadata.country();
		this.timeZone = metadata.timeZone();
		this.sourceMetadata = metadata.sourceMetadata();
		this.catalogConfirmed = true;
		this.catalogLastSeenAt = refreshedAt;
		this.catalogRefreshedAt = refreshedAt;
	}

	/**
	 * Applies administrator-supplied station metadata overrides.
	 * @param override the override
	 */
	public void applyOverride(StationOverride override) {
		if (override.alias() != null) {
			this.alias = override.alias();
		}
		String preferredName = override.preferredName();
		if (preferredName != null) {
			this.displayName = preferredName;
		}
		if (override.island() != null) {
			this.island = override.island();
		}
		if (override.region() != null) {
			this.region = override.region();
		}
		Boolean overrideEnabled = override.enabled();
		if (overrideEnabled != null) {
			this.enabled = overrideEnabled;
		}
		Boolean overrideFeatured = override.featured();
		if (overrideFeatured != null) {
			this.featured = overrideFeatured;
		}
		if (override.disabledReason() != null) {
			this.disabledReason = override.disabledReason();
		}
		String overridePrecipitationKey = override.precipitationKey();
		if (overridePrecipitationKey != null) {
			this.precipitationKey = overridePrecipitationKey;
			if (overridePrecipitationKey.startsWith("PC")) {
				this.rainfallCapability = RainfallCapability.SUPPORTED_ACCUMULATOR;
			}
			else if (ShefPrecipitationCode.fixedInterval(overridePrecipitationKey).isPresent()) {
				this.rainfallCapability = RainfallCapability.SUPPORTED_INTERVAL_PRECIPITATION;
			}
			else {
				this.rainfallCapability = RainfallCapability.PRECIPITATION_TYPE_UNKNOWN;
			}
		}
		if (override.note() != null) {
			this.overrideNote = override.note();
		}
	}

	/**
	 * Updates capability.
	 * @param capability the capability
	 * @param precipitationKey the precipitation key
	 */
	public void updateCapability(RainfallCapability capability, @Nullable String precipitationKey) {
		this.rainfallCapability = capability;
		this.precipitationKey = precipitationKey;
	}

	/**
	 * Marks not seen during refresh.
	 * @param refreshedAt the refreshed at
	 */
	public void markNotSeenDuringRefresh(Instant refreshedAt) {
		this.catalogConfirmed = false;
		this.catalogRefreshedAt = refreshedAt;
	}

	/**
	 * Records latest observation.
	 * @param latestObservationAt the latest observation timestamp
	 */
	public void recordLatestObservation(Instant latestObservationAt) {
		if (this.latestObservationAt == null || latestObservationAt.isAfter(this.latestObservationAt)) {
			this.latestObservationAt = latestObservationAt;
		}
	}

	/**
	 * Returns the ID.
	 * @return the ID
	 */
	public @Nullable UUID getId() {
		return this.id;
	}

	/**
	 * Returns the network.
	 * @return the network
	 */
	public String getNetwork() {
		return this.network;
	}

	/**
	 * Returns the station ID.
	 * @return the station ID
	 */
	public String getStationId() {
		return this.stationId;
	}

	/**
	 * Returns the source name.
	 * @return the source name
	 */
	public String getSourceName() {
		return this.sourceName;
	}

	/**
	 * Returns the display name.
	 * @return the display name
	 */
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Returns the alias.
	 * @return the alias
	 */
	public @Nullable String getAlias() {
		return this.alias;
	}

	/**
	 * Returns the island.
	 * @return the island
	 */
	public @Nullable String getIsland() {
		return this.island;
	}

	/**
	 * Returns the region.
	 * @return the region
	 */
	public @Nullable String getRegion() {
		return this.region;
	}

	/**
	 * Returns the latitude.
	 * @return the latitude
	 */
	public @Nullable BigDecimal getLatitude() {
		return this.latitude;
	}

	/**
	 * Returns the longitude.
	 * @return the longitude
	 */
	public @Nullable BigDecimal getLongitude() {
		return this.longitude;
	}

	/**
	 * Returns the elevation.
	 * @return the elevation
	 */
	public @Nullable BigDecimal getElevation() {
		return this.elevation;
	}

	/**
	 * Returns the archive begin.
	 * @return the archive begin
	 */
	public @Nullable LocalDate getArchiveBegin() {
		return this.archiveBegin;
	}

	/**
	 * Returns the archive end.
	 * @return the archive end
	 */
	public @Nullable LocalDate getArchiveEnd() {
		return this.archiveEnd;
	}

	/**
	 * Returns whether source online.
	 * @return {@code true} if source online; otherwise {@code false}
	 */
	public boolean isSourceOnline() {
		return this.sourceOnline;
	}

	/**
	 * Returns whether enabled.
	 * @return {@code true} if enabled; otherwise {@code false}
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Returns whether featured.
	 * @return {@code true} if featured; otherwise {@code false}
	 */
	public boolean isFeatured() {
		return this.featured;
	}

	/**
	 * Returns whether catalog confirmed.
	 * @return {@code true} if catalog confirmed; otherwise {@code false}
	 */
	public boolean isCatalogConfirmed() {
		return this.catalogConfirmed;
	}

	/**
	 * Returns the rainfall capability.
	 * @return the rainfall capability
	 */
	public RainfallCapability getRainfallCapability() {
		return this.rainfallCapability;
	}

	/**
	 * Returns the precipitation key.
	 * @return the precipitation key
	 */
	public @Nullable String getPrecipitationKey() {
		return this.precipitationKey;
	}

	/**
	 * Returns the state.
	 * @return the state
	 */
	public @Nullable String getState() {
		return this.state;
	}

	/**
	 * Returns the country.
	 * @return the country
	 */
	public @Nullable String getCountry() {
		return this.country;
	}

	/**
	 * Returns the time zone.
	 * @return the time zone
	 */
	public @Nullable String getTimeZone() {
		return this.timeZone;
	}

	/**
	 * Returns the disabled reason.
	 * @return the disabled reason
	 */
	public @Nullable String getDisabledReason() {
		return this.disabledReason;
	}

	/**
	 * Returns the override note.
	 * @return the override note
	 */
	public @Nullable String getOverrideNote() {
		return this.overrideNote;
	}

	/**
	 * Returns the source metadata.
	 * @return the source metadata
	 */
	public @Nullable String getSourceMetadata() {
		return this.sourceMetadata;
	}

	/**
	 * Returns the latest observation at.
	 * @return the latest observation at
	 */
	public @Nullable Instant getLatestObservationAt() {
		return this.latestObservationAt;
	}

	/**
	 * Returns the catalog last seen at.
	 * @return the catalog last seen at
	 */
	public @Nullable Instant getCatalogLastSeenAt() {
		return this.catalogLastSeenAt;
	}

	/**
	 * Returns the catalog refreshed at.
	 * @return the catalog refreshed at
	 */
	public @Nullable Instant getCatalogRefreshedAt() {
		return this.catalogRefreshedAt;
	}

	/**
	 * Describes source-supplied station metadata.
	 * @param sourceName the source name
	 * @param latitude the latitude
	 * @param longitude the longitude
	 * @param elevation the elevation
	 * @param sourceOnline the source online
	 * @param archiveBegin the archive begin
	 * @param archiveEnd the archive end
	 * @param state the state
	 * @param country the country
	 * @param timeZone the time zone
	 * @param sourceMetadata the source metadata
	 */
	public record SourceMetadata(
			String sourceName,
			@Nullable BigDecimal latitude,
			@Nullable BigDecimal longitude,
			@Nullable BigDecimal elevation,
			boolean sourceOnline,
			@Nullable LocalDate archiveBegin,
			@Nullable LocalDate archiveEnd,
			@Nullable String state,
			@Nullable String country,
			@Nullable String timeZone,
			@Nullable String sourceMetadata) {
	}

}
