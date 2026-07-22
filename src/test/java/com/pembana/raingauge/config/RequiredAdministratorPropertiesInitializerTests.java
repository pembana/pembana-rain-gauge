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

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextException;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests required administrator properties initializer.
 * @author Gunnar Hillert
 */
class RequiredAdministratorPropertiesInitializerTests {

	private static final String USERNAME_PROPERTY = RequiredAdministratorPropertiesInitializer.USERNAME_PROPERTY;

	private static final String PASSWORD_PROPERTY = RequiredAdministratorPropertiesInitializer.PASSWORD_PROPERTY;

	/**
	 * Verifies that accepts configured administrator properties.
	 */
	@Test
	void acceptsConfiguredAdministratorProperties() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(USERNAME_PROPERTY, "administrator")
				.withProperty(PASSWORD_PROPERTY, "{noop}secret");

		assertDoesNotThrow(() -> RequiredAdministratorPropertiesInitializer.validate(environment));
	}

	/**
	 * Verifies that rejects missing administrator properties without exposing values.
	 */
	@Test
	void rejectsMissingAdministratorPropertiesWithoutExposingValues() {
		ApplicationContextException exception = assertThrows(ApplicationContextException.class,
				() -> RequiredAdministratorPropertiesInitializer.validate(new MockEnvironment()));

		assertEquals("Required configuration properties are missing or blank: "
				+ "hawaii.rainfall.administrator.username, "
				+ "hawaii.rainfall.administrator.password", exception.getMessage());
	}

	/**
	 * Verifies that rejects blank administrator property.
	 */
	@Test
	void rejectsBlankAdministratorProperty() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(USERNAME_PROPERTY, "  ")
				.withProperty(PASSWORD_PROPERTY, "{noop}secret");

		ApplicationContextException exception = assertThrows(ApplicationContextException.class,
				() -> RequiredAdministratorPropertiesInitializer.validate(environment));

		assertEquals("Required configuration properties are missing or blank: "
				+ "hawaii.rainfall.administrator.username", exception.getMessage());
	}

}
