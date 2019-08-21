package org.continuity.orchestrator.config;

import org.continuity.commons.idpa.AppIdConverter;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

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

	@Bean
	Module jdk8Module() {
		return new Jdk8Module();
	}

}
