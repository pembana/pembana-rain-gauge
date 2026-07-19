package com.pembana.raingauge;

import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.config.RequiredAdministratorPropertiesInitializer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(RainfallProperties.class)
public class PembanaRainGaugeApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(PembanaRainGaugeApplication.class);
		application.addInitializers(new RequiredAdministratorPropertiesInitializer());
		application.run(args);
	}

}
