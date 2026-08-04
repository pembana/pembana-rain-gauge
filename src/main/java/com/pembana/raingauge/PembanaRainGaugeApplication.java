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

package com.pembana.raingauge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.config.RequiredAdministratorPropertiesInitializer;

/**
 * Bootstraps the Pembana Rain Gauge Spring application.
 * @author Gunnar Hillert
 */
@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(RainfallProperties.class)
public class PembanaRainGaugeApplication {

	/**
	 * Starts the Pembana Rain Gauge application.
	 * @param args the command-line arguments
	 */
	static void main(String[] args) {
		SpringApplication application = new SpringApplication(PembanaRainGaugeApplication.class);
		application.addInitializers(new RequiredAdministratorPropertiesInitializer());
		application.run(args);
	}

}
