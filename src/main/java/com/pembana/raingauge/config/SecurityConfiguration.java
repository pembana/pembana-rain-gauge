package com.pembana.raingauge.config;

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
						"/favicon.ico", "/error")
				.permitAll()
				.anyRequest().denyAll());
		configureHeaders(http);
		return http.build();
	}

	private void configureHeaders(HttpSecurity http) throws Exception {
		http.headers((headers) -> headers.contentSecurityPolicy(
					(policy) -> policy.policyDirectives(
							"default-src 'self'; script-src 'self'; style-src 'self'; "
							+ "img-src 'self' data:; connect-src 'self'; font-src 'self'; "
							+ "object-src 'none'; base-uri 'self'; "
							+ "frame-ancestors 'none'")));
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
