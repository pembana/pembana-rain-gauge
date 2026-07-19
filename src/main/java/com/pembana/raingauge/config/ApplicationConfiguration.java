package com.pembana.raingauge.config;

import java.time.Clock;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	CacheManager cacheManager(RainfallProperties properties) {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		register(cacheManager, "stationVariables", properties.getCache().getStationVariables());
		register(cacheManager, "dashboard", properties.getCache().getDashboard());
		register(cacheManager, "dailySummaries", properties.getCache().getDailySummaries());
		return cacheManager;
	}

	private void register(CaffeineCacheManager cacheManager, String name,
			java.time.Duration expiration) {
		cacheManager.registerCustomCache(name, Caffeine.newBuilder()
				.maximumSize(2_000)
				.expireAfterWrite(expiration)
				.recordStats()
				.build());
	}

}
