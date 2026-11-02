package com.srm.creditengine.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

	@Bean
	Clock clock() {
		return Clock.system(BUSINESS_ZONE);
	}

}
