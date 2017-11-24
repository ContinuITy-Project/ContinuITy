package org.continuity.frontend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class RestConfig {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
