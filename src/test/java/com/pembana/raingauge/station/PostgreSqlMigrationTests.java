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

package com.pembana.raingauge.station;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests PostgreSQL migration.
 * @author Gunnar Hillert
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "hawaii.rainfall.catalog.startup-enabled=false")
class PostgreSqlMigrationTests {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

	/**
	 * Registers the PostgreSQL test-container connection properties.
	 * @param registry the dynamic property registry
	 */
	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private StationRepository repository;

	/**
	 * Verifies that flyway migration and repository work on PostgreSQL.
	 */
	@Test
	void flywayMigrationAndRepositoryWorkOnPostgreSql() {
		Station station = this.repository.saveAndFlush(new Station("HI_DCP", "PGTH1",
				"PostgreSQL migration fixture"));

		assertThat(this.repository.findById(station.getId())).isPresent();
	}

}
