package org.continuity.request.rates.config;

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
	RestTemplate plainRestTemplate() {
		return new RestTemplate();
	}

}
