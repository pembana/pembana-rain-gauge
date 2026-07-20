package com.pembana.raingauge.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.pembana.raingauge.station.StationOverride;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("hawaii.rainfall")
public class RainfallProperties {

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
	private final Reset reset = new Reset();

	@Valid
	private final Administrator administrator = new Administrator();

	private final Map<String, StationOverride> stationOverrides = new LinkedHashMap<>();

	public Catalog getCatalog() {
		return this.catalog;
	}

	public Providers getProviders() {
		return this.providers;
	}

	public Cache getCache() {
		return this.cache;
	}

	public Query getQuery() {
		return this.query;
	}

	public Dashboard getDashboard() {
		return this.dashboard;
	}

	public Reset getReset() {
		return this.reset;
	}

	public Administrator getAdministrator() {
		return this.administrator;
	}

	public Map<String, StationOverride> getStationOverrides() {
		return this.stationOverrides;
	}

	public static class Catalog {

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

		public String getNetwork() {
			return this.network;
		}

		public void setNetwork(String network) {
			this.network = network;
		}

		public Duration getRefreshInterval() {
			return this.refreshInterval;
		}

		public void setRefreshInterval(Duration refreshInterval) {
			this.refreshInterval = refreshInterval;
		}

		public Duration getRefreshInitialDelay() {
			return this.refreshInitialDelay;
		}

		public void setRefreshInitialDelay(Duration refreshInitialDelay) {
			this.refreshInitialDelay = refreshInitialDelay;
		}

		public Duration getCapabilityRefreshInterval() {
			return this.capabilityRefreshInterval;
		}

		public void setCapabilityRefreshInterval(Duration capabilityRefreshInterval) {
			this.capabilityRefreshInterval = capabilityRefreshInterval;
		}

		public Duration getCapabilityRefreshInitialDelay() {
			return this.capabilityRefreshInitialDelay;
		}

		public void setCapabilityRefreshInitialDelay(Duration capabilityRefreshInitialDelay) {
			this.capabilityRefreshInitialDelay = capabilityRefreshInitialDelay;
		}

		public int getCapabilityRefreshConcurrency() {
			return this.capabilityRefreshConcurrency;
		}

		public void setCapabilityRefreshConcurrency(int capabilityRefreshConcurrency) {
			this.capabilityRefreshConcurrency = capabilityRefreshConcurrency;
		}

		public boolean isFailStartupWhenEmpty() {
			return this.failStartupWhenEmpty;
		}

		public void setFailStartupWhenEmpty(boolean failStartupWhenEmpty) {
			this.failStartupWhenEmpty = failStartupWhenEmpty;
		}

		public boolean isStartupEnabled() {
			return this.startupEnabled;
		}

		public void setStartupEnabled(boolean startupEnabled) {
			this.startupEnabled = startupEnabled;
		}

	}

	public static class Providers {

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

		public String getIemBaseUrl() {
			return this.iemBaseUrl;
		}

		public void setIemBaseUrl(String iemBaseUrl) {
			this.iemBaseUrl = iemBaseUrl;
		}

		public String getHadsBaseUrl() {
			return this.hadsBaseUrl;
		}

		public void setHadsBaseUrl(String hadsBaseUrl) {
			this.hadsBaseUrl = hadsBaseUrl;
		}

		public Duration getConnectTimeout() {
			return this.connectTimeout;
		}

		public void setConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public Duration getReadTimeout() {
			return this.readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

		public int getRetries() {
			return this.retries;
		}

		public void setRetries(int retries) {
			this.retries = retries;
		}

		public Duration getRetryInitialBackoff() {
			return this.retryInitialBackoff;
		}

		public void setRetryInitialBackoff(Duration retryInitialBackoff) {
			this.retryInitialBackoff = retryInitialBackoff;
		}

		public int getMaximumPayloadBytes() {
			return this.maximumPayloadBytes;
		}

		public void setMaximumPayloadBytes(int maximumPayloadBytes) {
			this.maximumPayloadBytes = maximumPayloadBytes;
		}

		public String getUserAgent() {
			return this.userAgent;
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}

	}

	public static class Cache {

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

		public Duration getObservations() {
			return this.observations;
		}

		public void setObservations(Duration observations) {
			this.observations = observations;
		}

		public Duration getHistoricalObservations() {
			return this.historicalObservations;
		}

		public void setHistoricalObservations(Duration historicalObservations) {
			this.historicalObservations = historicalObservations;
		}

		public Duration getStaleObservations() {
			return this.staleObservations;
		}

		public void setStaleObservations(Duration staleObservations) {
			this.staleObservations = staleObservations;
		}

		public Duration getDashboard() {
			return this.dashboard;
		}

		public void setDashboard(Duration dashboard) {
			this.dashboard = dashboard;
		}

		public Duration getStationVariables() {
			return this.stationVariables;
		}

		public void setStationVariables(Duration stationVariables) {
			this.stationVariables = stationVariables;
		}

		public Duration getDailySummaries() {
			return this.dailySummaries;
		}

		public void setDailySummaries(Duration dailySummaries) {
			this.dailySummaries = dailySummaries;
		}

	}

	public static class Query {

		@NotNull
		private Duration maximumRange = Duration.ofDays(366);

		@NotNull
		private Duration baselineLookback = Duration.ofHours(6);

		public Duration getMaximumRange() {
			return this.maximumRange;
		}

		public void setMaximumRange(Duration maximumRange) {
			this.maximumRange = maximumRange;
		}

		public Duration getBaselineLookback() {
			return this.baselineLookback;
		}

		public void setBaselineLookback(Duration baselineLookback) {
			this.baselineLookback = baselineLookback;
		}

	}

	public static class Dashboard {

		@NotBlank
		private String defaultStation = "WIHH1";

		@NotBlank
		private String defaultPeriod = "28d";

		@NotBlank
		private String defaultUnit = "imperial";

		public String getDefaultStation() {
			return this.defaultStation;
		}

		public void setDefaultStation(String defaultStation) {
			this.defaultStation = defaultStation;
		}

		public String getDefaultPeriod() {
			return this.defaultPeriod;
		}

		public void setDefaultPeriod(String defaultPeriod) {
			this.defaultPeriod = defaultPeriod;
		}

		public String getDefaultUnit() {
			return this.defaultUnit;
		}

		public void setDefaultUnit(String defaultUnit) {
			this.defaultUnit = defaultUnit;
		}

	}

	public static class Reset {

		@NotNull
		private BigDecimal nearZeroThreshold = new BigDecimal("0.05");

		@NotNull
		private BigDecimal rolloverMaximum = new BigDecimal("100.00");

		@NotNull
		private BigDecimal suspectedOutlierIncrement = new BigDecimal("5.00");

		public BigDecimal getNearZeroThreshold() {
			return this.nearZeroThreshold;
		}

		public void setNearZeroThreshold(BigDecimal nearZeroThreshold) {
			this.nearZeroThreshold = nearZeroThreshold;
		}

		public BigDecimal getRolloverMaximum() {
			return this.rolloverMaximum;
		}

		public void setRolloverMaximum(BigDecimal rolloverMaximum) {
			this.rolloverMaximum = rolloverMaximum;
		}

		public BigDecimal getSuspectedOutlierIncrement() {
			return this.suspectedOutlierIncrement;
		}

		public void setSuspectedOutlierIncrement(BigDecimal suspectedOutlierIncrement) {
			this.suspectedOutlierIncrement = suspectedOutlierIncrement;
		}

	}

	public static class Administrator {

		@NotBlank
		private String username = "admin";

		@NotBlank
		private String password = "{noop}change-me";

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

}
