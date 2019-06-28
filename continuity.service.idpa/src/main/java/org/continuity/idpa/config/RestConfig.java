package org.continuity.idpa.config;

import org.continuity.commons.idpa.AppIdConverter;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class RestConfig {

	@LoadBalanced
	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	AppIdConverter appIdConverter() {
		return new AppIdConverter();
	}

}
