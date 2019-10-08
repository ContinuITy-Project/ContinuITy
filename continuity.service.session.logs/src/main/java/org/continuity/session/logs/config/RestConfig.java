package org.continuity.session.logs.config;

import org.continuity.commons.idpa.AppIdConverter;
import org.continuity.commons.idpa.VersionOrTimestampConverter;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

	@LoadBalanced
	@Bean
	@Primary
	RestTemplate eurekaRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	RestTemplate plainRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	AppIdConverter appIdConverter() {
		return new AppIdConverter();
	}

	@Bean
	VersionOrTimestampConverter versionOrTimestampConverter() {
		return new VersionOrTimestampConverter();
	}

}
