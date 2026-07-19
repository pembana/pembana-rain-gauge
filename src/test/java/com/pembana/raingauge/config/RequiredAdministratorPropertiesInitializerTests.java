package com.pembana.raingauge.config;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContextException;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequiredAdministratorPropertiesInitializerTests {

	private static final String USERNAME_PROPERTY = RequiredAdministratorPropertiesInitializer.USERNAME_PROPERTY;

	private static final String PASSWORD_PROPERTY = RequiredAdministratorPropertiesInitializer.PASSWORD_PROPERTY;

	@Test
	void acceptsConfiguredAdministratorProperties() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(USERNAME_PROPERTY, "administrator")
				.withProperty(PASSWORD_PROPERTY, "{noop}secret");

		assertDoesNotThrow(() -> RequiredAdministratorPropertiesInitializer.validate(environment));
	}

	@Test
	void rejectsMissingAdministratorPropertiesWithoutExposingValues() {
		ApplicationContextException exception = assertThrows(ApplicationContextException.class,
				() -> RequiredAdministratorPropertiesInitializer.validate(new MockEnvironment()));

		assertEquals("Required configuration properties are missing or blank: "
				+ "hawaii.rainfall.administrator.username, "
				+ "hawaii.rainfall.administrator.password", exception.getMessage());
	}

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
