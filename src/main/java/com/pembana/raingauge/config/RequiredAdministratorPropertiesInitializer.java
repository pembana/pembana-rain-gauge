package com.pembana.raingauge.config;

import java.util.List;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public final class RequiredAdministratorPropertiesInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	static final String USERNAME_PROPERTY = "hawaii.rainfall.administrator.username";

	static final String PASSWORD_PROPERTY = "hawaii.rainfall.administrator.password";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		validate(applicationContext.getEnvironment());
	}

	static void validate(Environment environment) {
		List<String> missingProperties = List.of(USERNAME_PROPERTY, PASSWORD_PROPERTY).stream()
				.filter((property) -> !hasText(environment, property))
				.toList();
		if (!missingProperties.isEmpty()) {
			throw new ApplicationContextException(
					"Required configuration properties are missing or blank: "
					+ String.join(", ", missingProperties));
		}
	}

	private static boolean hasText(Environment environment, String property) {
		try {
			return StringUtils.hasText(environment.getProperty(property));
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

}
