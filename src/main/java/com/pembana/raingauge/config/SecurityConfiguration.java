package com.pembana.raingauge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((requests) -> requests
				.requestMatchers("/", "/stations", "/stations/**", "/compare", "/about-data",
						"/api/stations/**", "/api/compare", "/actuator/health")
				.permitAll()
				.requestMatchers("/css/**", "/js/**", "/vendor/**", "/favicon.svg")
				.permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().denyAll())
			.httpBasic(Customizer.withDefaults())
			.headers((headers) -> headers.contentSecurityPolicy(
					(policy) -> policy.policyDirectives(
							"default-src 'self'; script-src 'self'; style-src 'self'; "
							+ "img-src 'self' data:; connect-src 'self'; font-src 'self'; "
							+ "object-src 'none'; base-uri 'self'; "
							+ "frame-ancestors 'none'")));
		return http.build();
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
