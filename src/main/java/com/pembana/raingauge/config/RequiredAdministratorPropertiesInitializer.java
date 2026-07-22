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

import java.util.List;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Validates required administrator credentials before application startup.
 * @author Gunnar Hillert
 */
public final class RequiredAdministratorPropertiesInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	/** Creates the administrator-property initializer. */
	public RequiredAdministratorPropertiesInitializer() {
	}

	static final String USERNAME_PROPERTY = "hawaii.rainfall.administrator.username";

	static final String PASSWORD_PROPERTY = "hawaii.rainfall.administrator.password";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		validate(applicationContext.getEnvironment());
	}

	/**
	 * Validates that required administrator credentials are configured.
	 * @param environment the environment containing application properties
	 */
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

	/**
	 * Determines whether has text.
	 * @param environment the environment containing application properties
	 * @param property the property
	 * @return {@code true} when has text; otherwise {@code false}
	 */
	private static boolean hasText(Environment environment, String property) {
		try {
			return StringUtils.hasText(environment.getProperty(property));
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

}
