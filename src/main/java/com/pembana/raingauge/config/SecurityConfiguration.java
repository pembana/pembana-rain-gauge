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

import java.net.URI;
import java.util.Locale;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures public and administrative web security.
 * @author Gunnar Hillert
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	private final RainfallProperties properties;

	/**
	 * Creates a new {@code SecurityConfiguration}.
	 * @param properties the rainfall application properties
	 */
	SecurityConfiguration(RainfallProperties properties) {
		this.properties = properties;
	}

	/**
	 * Configures security for administrative endpoints.
	 * @param http the HTTP
	 * @return the resulting admin security filter chain
	 */
	@Bean
	@Order(1)
	SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) {
		http.securityMatcher("/admin/**")
			.authorizeHttpRequests((requests) -> requests.anyRequest().hasRole("ADMIN"))
			.httpBasic(Customizer.withDefaults());
		configureHeaders(http);
		return buildSecurityFilterChain(http);
	}

	/**
	 * Configures security for public application endpoints.
	 * @param http the HTTP
	 * @return the resulting public security filter chain
	 */
	@Bean
	@Order(2)
	SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) {
		http.authorizeHttpRequests((requests) -> requests
				.requestMatchers("/", "/stations", "/stations/**", "/compare", "/about-data",
						"/api/stations/**", "/api/compare", "/actuator/health")
				.permitAll()
				.requestMatchers("/css/**", "/js/**", "/vendor/**", "/favicon.svg",
						"/favicon.ico", "/social-image.png", "/error")
				.permitAll()
				.anyRequest().denyAll());
		configureHeaders(http);
		return buildSecurityFilterChain(http);
	}

	/**
	 * Builds a configured security filter chain.
	 * @param http the HTTP security builder
	 * @return the resulting security filter chain
	 */
	private SecurityFilterChain buildSecurityFilterChain(HttpSecurity http) {
		try {
			return http.build();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Unable to build security filter chain", ex);
		}
	}

	/**
	 * Configures headers.
	 * @param http the HTTP
	 */
	private void configureHeaders(HttpSecurity http) {
		String tileImageSource = stationMapTileImageSource();
		http.headers((headers) -> headers.contentSecurityPolicy(
					(policy) -> policy.policyDirectives(
							"default-src 'self'; script-src 'self'; style-src 'self'; "
							+ "img-src 'self' data:" + tileImageSource
							+ "; connect-src 'self'; font-src 'self'; "
							+ "object-src 'none'; base-uri 'self'; "
							+ "frame-ancestors 'none'")));
	}

	/**
	 * Builds the content-security-policy source for map tiles.
	 * @return the resulting station map tile image source
	 */
	private String stationMapTileImageSource() {
		String tileUrl = this.properties.getStationMap().getTileUrl();
		if (tileUrl.startsWith("/")) {
			return "";
		}
		try {
			URI uri = URI.create(tileUrl.replaceAll("\\{[^}]+}", "0"));
			String scheme = uri.getScheme();
			if (scheme == null || uri.getHost() == null
					|| !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
				throw new IllegalArgumentException("URL must use HTTP or HTTPS and include a host");
			}
			String host = uri.getHost().toLowerCase(Locale.ROOT);
			if (host.contains(":")) {
				host = "[" + host + "]";
			}
			String port = (uri.getPort() == -1) ? "" : ":" + uri.getPort();
			return " " + scheme.toLowerCase(Locale.ROOT) + "://" + host + port;
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"hawaii.rainfall.station-map.tile-url must be a relative "
					+ "or HTTP(S) URL template",
					ex);
		}
	}

	/**
	 * Creates the configured administrator account.
	 * @param properties the rainfall application properties
	 * @return the resulting user details service
	 */
	@Bean
	UserDetailsService userDetailsService(RainfallProperties properties) {
		RainfallProperties.Administrator administrator = properties.getAdministrator();
		return new InMemoryUserDetailsManager(User.withUsername(administrator.getUsername())
				.password(administrator.getPassword())
				.roles("ADMIN")
				.build());
	}

}
