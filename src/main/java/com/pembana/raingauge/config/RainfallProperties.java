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

package com.pembana.raingauge.config;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.pembana.raingauge.station.StationOverride;

/**
 * Defines bindable rainfall configuration properties.
 * @author Gunnar Hillert
 */
@Validated
@ConfigurationProperties("hawaii.rainfall")
public class RainfallProperties {

	/** Creates the rainfall configuration properties. */
	public RainfallProperties() {
	}

	@Valid
	private final Catalog catalog = new Catalog();

	@Valid
	private final Providers providers = new Providers();

	@Valid
	private final Cache cache = new Cache();

	@Valid
	private final Query query = new Query();

	@Valid
	private final Dashboard dashboard = new Dashboard();

	@Valid
	private final Site site = new Site();

	@Valid
	private final StationMap stationMap = new StationMap();

	@Valid
	private final Reset reset = new Reset();

	@Valid
	private final Administrator administrator = new Administrator();

	private final Map<String, StationOverride> stationOverrides = new LinkedHashMap<>();

	/**
	 * Returns the catalog.
	 * @return the catalog
	 */
	public Catalog getCatalog() {
		return this.catalog;
	}

	/**
	 * Returns the providers.
	 * @return the providers
	 */
	public Providers getProviders() {
		return this.providers;
	}

	/**
	 * Returns the cache.
	 * @return the cache
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * Returns the query.
	 * @return the query
	 */
	public Query getQuery() {
		return this.query;
	}

	/**
	 * Returns the dashboard.
	 * @return the dashboard
	 */
	public Dashboard getDashboard() {
		return this.dashboard;
	}

	/**
	 * Returns the site.
	 * @return the site
	 */
	public Site getSite() {
		return this.site;
	}

	/**
	 * Returns the station map.
	 * @return the station map
	 */
	public StationMap getStationMap() {
		return this.stationMap;
	}

	/**
	 * Returns the reset.
	 * @return the reset
	 */
	public Reset getReset() {
		return this.reset;
	}

	/**
	 * Returns the administrator.
	 * @return the administrator
	 */
	public Administrator getAdministrator() {
		return this.administrator;
	}

	/**
	 * Returns the station overrides.
	 * @return the station overrides
	 */
	public Map<String, StationOverride> getStationOverrides() {
		return this.stationOverrides;
	}

	/**
	 * Groups the catalog configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Catalog {

		/** Creates the catalog configuration properties. */
		public Catalog() {
		}

		@NotBlank
		private String network = "HI_DCP";

		@NotNull
		private Duration refreshInterval = Duration.ofHours(24);

		@NotNull
		private Duration refreshInitialDelay = Duration.ofHours(24);

		@NotNull
		private Duration capabilityRefreshInterval = Duration.ofHours(6);

		@NotNull
		private Duration capabilityRefreshInitialDelay = Duration.ofHours(6);

		@Min(1)
		@Max(32)
		private int capabilityRefreshConcurrency = 8;

		private boolean failStartupWhenEmpty;

		private boolean startupEnabled = true;

		/**
		 * Returns the network.
		 * @return the network
		 */
		public String getNetwork() {
			return this.network;
		}

		/**
		 * Sets the network.
		 * @param network the provider network identifier
		 */
		public void setNetwork(String network) {
			this.network = network;
		}

		/**
		 * Returns the refresh interval.
		 * @return the refresh interval
		 */
		public Duration getRefreshInterval() {
			return this.refreshInterval;
		}

		/**
		 * Sets the refresh interval.
		 * @param refreshInterval the refresh interval
		 */
		public void setRefreshInterval(Duration refreshInterval) {
			this.refreshInterval = refreshInterval;
		}

		/**
		 * Returns the refresh initial delay.
		 * @return the refresh initial delay
		 */
		public Duration getRefreshInitialDelay() {
			return this.refreshInitialDelay;
		}

		/**
		 * Sets the refresh initial delay.
		 * @param refreshInitialDelay the refresh initial delay
		 */
		public void setRefreshInitialDelay(Duration refreshInitialDelay) {
			this.refreshInitialDelay = refreshInitialDelay;
		}

		/**
		 * Returns the capability refresh interval.
		 * @return the capability refresh interval
		 */
		public Duration getCapabilityRefreshInterval() {
			return this.capabilityRefreshInterval;
		}

		/**
		 * Sets the capability refresh interval.
		 * @param capabilityRefreshInterval the capability refresh interval
		 */
		public void setCapabilityRefreshInterval(Duration capabilityRefreshInterval) {
			this.capabilityRefreshInterval = capabilityRefreshInterval;
		}

		/**
		 * Returns the capability refresh initial delay.
		 * @return the capability refresh initial delay
		 */
		public Duration getCapabilityRefreshInitialDelay() {
			return this.capabilityRefreshInitialDelay;
		}

		/**
		 * Sets the capability refresh initial delay.
		 * @param capabilityRefreshInitialDelay the capability refresh initial delay
		 */
		public void setCapabilityRefreshInitialDelay(Duration capabilityRefreshInitialDelay) {
			this.capabilityRefreshInitialDelay = capabilityRefreshInitialDelay;
		}

		/**
		 * Returns the capability refresh concurrency.
		 * @return the capability refresh concurrency
		 */
		public int getCapabilityRefreshConcurrency() {
			return this.capabilityRefreshConcurrency;
		}

		/**
		 * Sets the capability refresh concurrency.
		 * @param capabilityRefreshConcurrency the capability refresh concurrency
		 */
		public void setCapabilityRefreshConcurrency(int capabilityRefreshConcurrency) {
			this.capabilityRefreshConcurrency = capabilityRefreshConcurrency;
		}

		/**
		 * Returns whether fail startup when empty.
		 * @return {@code true} if fail startup when empty; otherwise {@code false}
		 */
		public boolean isFailStartupWhenEmpty() {
			return this.failStartupWhenEmpty;
		}

		/**
		 * Sets the fail startup when empty.
		 * @param failStartupWhenEmpty the fail startup when empty
		 */
		public void setFailStartupWhenEmpty(boolean failStartupWhenEmpty) {
			this.failStartupWhenEmpty = failStartupWhenEmpty;
		}

		/**
		 * Returns whether startup enabled.
		 * @return {@code true} if startup enabled; otherwise {@code false}
		 */
		public boolean isStartupEnabled() {
			return this.startupEnabled;
		}

		/**
		 * Sets the startup enabled.
		 * @param startupEnabled the startup enabled
		 */
		public void setStartupEnabled(boolean startupEnabled) {
			this.startupEnabled = startupEnabled;
		}

	}

	/**
	 * Groups the providers configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Providers {

		/** Creates the provider configuration properties. */
		public Providers() {
		}

		@NotBlank
		private String iemBaseUrl = "https://mesonet.agron.iastate.edu";

		@NotBlank
		private String hadsBaseUrl = "https://mesonet.agron.iastate.edu";

		@NotNull
		private Duration connectTimeout = Duration.ofSeconds(5);

		@NotNull
		private Duration readTimeout = Duration.ofSeconds(30);

		@Min(0)
		@Max(4)
		private int retries = 2;

		@NotNull
		private Duration retryInitialBackoff = Duration.ofMillis(200);

		@Min(1024)
		private int maximumPayloadBytes = 5_000_000;

		@NotBlank
		private String userAgent = "Pembana-Rain-Gauge/0.1 (+https://github.com/pembana/pembana-rain-gauge)";

		/**
		 * Returns the IEM base URL.
		 * @return the IEM base URL
		 */
		public String getIemBaseUrl() {
			return this.iemBaseUrl;
		}

		/**
		 * Sets the IEM base URL.
		 * @param iemBaseUrl the IEM base URL
		 */
		public void setIemBaseUrl(String iemBaseUrl) {
			this.iemBaseUrl = iemBaseUrl;
		}

		/**
		 * Returns the HADS base URL.
		 * @return the HADS base URL
		 */
		public String getHadsBaseUrl() {
			return this.hadsBaseUrl;
		}

		/**
		 * Sets the HADS base URL.
		 * @param hadsBaseUrl the HADS base URL
		 */
		public void setHadsBaseUrl(String hadsBaseUrl) {
			this.hadsBaseUrl = hadsBaseUrl;
		}

		/**
		 * Returns the connect timeout.
		 * @return the connect timeout
		 */
		public Duration getConnectTimeout() {
			return this.connectTimeout;
		}

		/**
		 * Sets the connect timeout.
		 * @param connectTimeout the connect timeout
		 */
		public void setConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		/**
		 * Returns the read timeout.
		 * @return the read timeout
		 */
		public Duration getReadTimeout() {
			return this.readTimeout;
		}

		/**
		 * Sets the read timeout.
		 * @param readTimeout the read timeout
		 */
		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

		/**
		 * Returns the retries.
		 * @return the retries
		 */
		public int getRetries() {
			return this.retries;
		}

		/**
		 * Sets the retries.
		 * @param retries the retries
		 */
		public void setRetries(int retries) {
			this.retries = retries;
		}

		/**
		 * Returns the retry initial backoff.
		 * @return the retry initial backoff
		 */
		public Duration getRetryInitialBackoff() {
			return this.retryInitialBackoff;
		}

		/**
		 * Sets the retry initial backoff.
		 * @param retryInitialBackoff the retry initial backoff
		 */
		public void setRetryInitialBackoff(Duration retryInitialBackoff) {
			this.retryInitialBackoff = retryInitialBackoff;
		}

		/**
		 * Returns the maximum payload bytes.
		 * @return the maximum payload bytes
		 */
		public int getMaximumPayloadBytes() {
			return this.maximumPayloadBytes;
		}

		/**
		 * Sets the maximum payload bytes.
		 * @param maximumPayloadBytes the maximum payload bytes
		 */
		public void setMaximumPayloadBytes(int maximumPayloadBytes) {
			this.maximumPayloadBytes = maximumPayloadBytes;
		}

		/**
		 * Returns the user agent.
		 * @return the user agent
		 */
		public String getUserAgent() {
			return this.userAgent;
		}

		/**
		 * Sets the user agent.
		 * @param userAgent the user agent
		 */
		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}

	}

	/**
	 * Groups the cache configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Cache {

		/** Creates the cache configuration properties. */
		public Cache() {
		}

		@NotNull
		private Duration observations = Duration.ofMinutes(5);

		@NotNull
		private Duration historicalObservations = Duration.ofHours(24);

		@NotNull
		private Duration staleObservations = Duration.ofDays(7);

		@NotNull
		private Duration dashboard = Duration.ofMinutes(2);

		@NotNull
		private Duration stationVariables = Duration.ofHours(6);

		@NotNull
		private Duration dailySummaries = Duration.ofHours(6);

		/**
		 * Returns the observations.
		 * @return the observations
		 */
		public Duration getObservations() {
			return this.observations;
		}

		/**
		 * Sets the observations.
		 * @param observations the precipitation observations to process
		 */
		public void setObservations(Duration observations) {
			this.observations = observations;
		}

		/**
		 * Returns the historical observations.
		 * @return the historical observations
		 */
		public Duration getHistoricalObservations() {
			return this.historicalObservations;
		}

		/**
		 * Sets the historical observations.
		 * @param historicalObservations the historical observations
		 */
		public void setHistoricalObservations(Duration historicalObservations) {
			this.historicalObservations = historicalObservations;
		}

		/**
		 * Returns the stale observations.
		 * @return the stale observations
		 */
		public Duration getStaleObservations() {
			return this.staleObservations;
		}

		/**
		 * Sets the stale observations.
		 * @param staleObservations the stale observations
		 */
		public void setStaleObservations(Duration staleObservations) {
			this.staleObservations = staleObservations;
		}

		/**
		 * Returns the dashboard.
		 * @return the dashboard
		 */
		public Duration getDashboard() {
			return this.dashboard;
		}

		/**
		 * Sets the dashboard.
		 * @param dashboard the dashboard
		 */
		public void setDashboard(Duration dashboard) {
			this.dashboard = dashboard;
		}

		/**
		 * Returns the station variables.
		 * @return the station variables
		 */
		public Duration getStationVariables() {
			return this.stationVariables;
		}

		/**
		 * Sets the station variables.
		 * @param stationVariables the station variables
		 */
		public void setStationVariables(Duration stationVariables) {
			this.stationVariables = stationVariables;
		}

		/**
		 * Returns the daily summaries.
		 * @return the daily summaries
		 */
		public Duration getDailySummaries() {
			return this.dailySummaries;
		}

		/**
		 * Sets the daily summaries.
		 * @param dailySummaries the daily summaries
		 */
		public void setDailySummaries(Duration dailySummaries) {
			this.dailySummaries = dailySummaries;
		}

	}

	/**
	 * Groups the query configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Query {

		/** Creates the query configuration properties. */
		public Query() {
		}

		@NotNull
		private Duration maximumRange = Duration.ofDays(366);

		@NotNull
		private Duration baselineLookback = Duration.ofHours(6);

		/**
		 * Returns the maximum range.
		 * @return the maximum range
		 */
		public Duration getMaximumRange() {
			return this.maximumRange;
		}

		/**
		 * Sets the maximum range.
		 * @param maximumRange the maximum range
		 */
		public void setMaximumRange(Duration maximumRange) {
			this.maximumRange = maximumRange;
		}

		/**
		 * Returns the baseline lookback.
		 * @return the baseline lookback
		 */
		public Duration getBaselineLookback() {
			return this.baselineLookback;
		}

		/**
		 * Sets the baseline lookback.
		 * @param baselineLookback the baseline lookback
		 */
		public void setBaselineLookback(Duration baselineLookback) {
			this.baselineLookback = baselineLookback;
		}

	}

	/**
	 * Groups the dashboard configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Dashboard {

		/** Creates the dashboard configuration properties. */
		public Dashboard() {
		}

		@NotBlank
		private String defaultStation = "WIHH1";

		@NotBlank
		private String defaultPeriod = "28d";

		@NotBlank
		private String defaultUnit = "imperial";

		/**
		 * Returns the default station.
		 * @return the default station
		 */
		public String getDefaultStation() {
			return this.defaultStation;
		}

		/**
		 * Sets the default station.
		 * @param defaultStation the default station
		 */
		public void setDefaultStation(String defaultStation) {
			this.defaultStation = defaultStation;
		}

		/**
		 * Returns the default period.
		 * @return the default period
		 */
		public String getDefaultPeriod() {
			return this.defaultPeriod;
		}

		/**
		 * Sets the default period.
		 * @param defaultPeriod the default period
		 */
		public void setDefaultPeriod(String defaultPeriod) {
			this.defaultPeriod = defaultPeriod;
		}

		/**
		 * Returns the default unit.
		 * @return the default unit
		 */
		public String getDefaultUnit() {
			return this.defaultUnit;
		}

		/**
		 * Sets the default unit.
		 * @param defaultUnit the default unit
		 */
		public void setDefaultUnit(String defaultUnit) {
			this.defaultUnit = defaultUnit;
		}

	}

	/**
	 * Groups the station map configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class StationMap {

		/** Creates the station-map configuration properties. */
		public StationMap() {
		}

		@NotBlank
		private String tileUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";

		@NotBlank
		private String attributionLabel = "OpenStreetMap";

		@NotBlank
		private String attributionUrl = "https://www.openstreetmap.org/copyright";

		/**
		 * Returns the tile URL.
		 * @return the tile URL
		 */
		public String getTileUrl() {
			return this.tileUrl;
		}

		/**
		 * Sets the tile URL.
		 * @param tileUrl the tile URL
		 */
		public void setTileUrl(String tileUrl) {
			this.tileUrl = tileUrl;
		}

		/**
		 * Returns the attribution label.
		 * @return the attribution label
		 */
		public String getAttributionLabel() {
			return this.attributionLabel;
		}

		/**
		 * Sets the attribution label.
		 * @param attributionLabel the attribution label
		 */
		public void setAttributionLabel(String attributionLabel) {
			this.attributionLabel = attributionLabel;
		}

		/**
		 * Returns the attribution URL.
		 * @return the attribution URL
		 */
		public String getAttributionUrl() {
			return this.attributionUrl;
		}

		/**
		 * Sets the attribution URL.
		 * @param attributionUrl the attribution URL
		 */
		public void setAttributionUrl(String attributionUrl) {
			this.attributionUrl = attributionUrl;
		}

	}

	/**
	 * Groups the site configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Site {

		/** Creates the site configuration properties. */
		public Site() {
		}

		@NotNull
		private URI baseUrl = URI.create("http://localhost:8080");

		/**
		 * Returns the base URL.
		 * @return the base URL
		 */
		public URI getBaseUrl() {
			return this.baseUrl;
		}

		/**
		 * Sets the base URL.
		 * @param baseUrl the base URL
		 */
		public void setBaseUrl(URI baseUrl) {
			this.baseUrl = baseUrl;
		}

	}

	/**
	 * Groups the reset configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Reset {

		/** Creates the accumulator-reset configuration properties. */
		public Reset() {
		}

		@NotNull
		private BigDecimal nearZeroThreshold = new BigDecimal("0.05");

		@NotNull
		private BigDecimal rolloverMaximum = new BigDecimal("100.00");

		@NotNull
		private BigDecimal suspectedOutlierIncrement = new BigDecimal("5.00");

		/**
		 * Returns the near zero threshold.
		 * @return the near zero threshold
		 */
		public BigDecimal getNearZeroThreshold() {
			return this.nearZeroThreshold;
		}

		/**
		 * Sets the near zero threshold.
		 * @param nearZeroThreshold the near zero threshold
		 */
		public void setNearZeroThreshold(BigDecimal nearZeroThreshold) {
			this.nearZeroThreshold = nearZeroThreshold;
		}

		/**
		 * Returns the rollover maximum.
		 * @return the rollover maximum
		 */
		public BigDecimal getRolloverMaximum() {
			return this.rolloverMaximum;
		}

		/**
		 * Sets the rollover maximum.
		 * @param rolloverMaximum the rollover maximum
		 */
		public void setRolloverMaximum(BigDecimal rolloverMaximum) {
			this.rolloverMaximum = rolloverMaximum;
		}

		/**
		 * Returns the suspected outlier increment.
		 * @return the suspected outlier increment
		 */
		public BigDecimal getSuspectedOutlierIncrement() {
			return this.suspectedOutlierIncrement;
		}

		/**
		 * Sets the suspected outlier increment.
		 * @param suspectedOutlierIncrement the suspected outlier increment
		 */
		public void setSuspectedOutlierIncrement(BigDecimal suspectedOutlierIncrement) {
			this.suspectedOutlierIncrement = suspectedOutlierIncrement;
		}

	}

	/**
	 * Groups the administrator configuration properties.
	 * @author Gunnar Hillert
	 */
	public static class Administrator {

		/** Creates the administrator configuration properties. */
		public Administrator() {
		}

		@NotBlank
		private String username = "admin";

		@NotBlank
		private String password = "{noop}change-me";

		/**
		 * Returns the username.
		 * @return the username
		 */
		public String getUsername() {
			return this.username;
		}

		/**
		 * Sets the username.
		 * @param username the username
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * Returns the password.
		 * @return the password
		 */
		public String getPassword() {
			return this.password;
		}

		/**
		 * Sets the password.
		 * @param password the password
		 */
		public void setPassword(String password) {
			this.password = password;
		}

	}

}
