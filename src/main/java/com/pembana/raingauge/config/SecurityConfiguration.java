package com.pembana.raingauge.config;

import java.net.URI;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	private final RainfallProperties properties;

	SecurityConfiguration(RainfallProperties properties) {
		this.properties = properties;
	}

	@Bean
	@Order(1)
	SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/admin/**")
			.authorizeHttpRequests((requests) -> requests.anyRequest().hasRole("ADMIN"))
			.httpBasic(Customizer.withDefaults());
		configureHeaders(http);
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((requests) -> requests
				.requestMatchers("/", "/stations", "/stations/**", "/compare", "/about-data",
						"/api/stations/**", "/api/compare", "/actuator/health")
				.permitAll()
				.requestMatchers("/css/**", "/js/**", "/vendor/**", "/favicon.svg",
						"/favicon.ico", "/social-image.png", "/error")
				.permitAll()
				.anyRequest().denyAll());
		configureHeaders(http);
		return http.build();
	}

	private void configureHeaders(HttpSecurity http) throws Exception {
		String tileImageSource = stationMapTileImageSource();
		http.headers((headers) -> headers.contentSecurityPolicy(
					(policy) -> policy.policyDirectives(
							"default-src 'self'; script-src 'self'; style-src 'self'; "
							+ "img-src 'self' data:" + tileImageSource
							+ "; connect-src 'self'; font-src 'self'; "
							+ "object-src 'none'; base-uri 'self'; "
							+ "frame-ancestors 'none'")));
	}

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
			String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
			return " " + scheme.toLowerCase(Locale.ROOT) + "://" + host + port;
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"hawaii.rainfall.station-map.tile-url must be a relative "
					+ "or HTTP(S) URL template",
					ex);
		}
	}

	@Bean
	UserDetailsService userDetailsService(RainfallProperties properties) {
		RainfallProperties.Administrator administrator = properties.getAdministrator();
		return new InMemoryUserDetailsManager(User.withUsername(administrator.getUsername())
				.password(administrator.getPassword())
				.roles("ADMIN")
				.build());
	}

}
