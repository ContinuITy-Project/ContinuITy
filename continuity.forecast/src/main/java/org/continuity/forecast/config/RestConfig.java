package org.continuity.forecast.config;

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

}
