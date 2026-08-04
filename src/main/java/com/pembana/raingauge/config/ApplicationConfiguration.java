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

import java.time.Clock;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures clocks, caches, and asynchronous execution.
 * @author Gunnar Hillert
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfiguration {

	/**
	 * Creates the system clock used by application services.
	 * @return the resulting clock
	 */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	/**
	 * Creates the application cache manager.
	 * @param properties the rainfall application properties
	 * @return the resulting cache manager
	 */
	@Bean
	CacheManager cacheManager(RainfallProperties properties) {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		register(cacheManager, "stationVariables", properties.getCache().getStationVariables());
		register(cacheManager, "dashboard", properties.getCache().getDashboard());
		register(cacheManager, "dailySummaries", properties.getCache().getDailySummaries());
		return cacheManager;
	}

	/**
	 * Registers an observation batch in the interval index.
	 * @param cacheManager the cache manager
	 * @param name the name
	 * @param expiration the expiration
	 */
	private void register(CaffeineCacheManager cacheManager, String name,
			Duration expiration) {
		cacheManager.registerCustomCache(name, Caffeine.newBuilder()
				.maximumSize(2_000)
				.expireAfterWrite(expiration)
				.recordStats()
				.build());
	}

}
