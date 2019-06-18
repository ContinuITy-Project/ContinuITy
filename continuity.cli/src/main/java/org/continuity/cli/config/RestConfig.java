package org.continuity.cli.config;

import org.continuity.idpa.serialization.IdpaSerializationUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Bean
	@Primary
	ObjectMapper yamlObjectMapper() {
		return IdpaSerializationUtils.getDefaultYamlObjectMapper();
	}

	@Bean
	ObjectMapper jsonObjectMapper() {
		return IdpaSerializationUtils.getDefaultJsonObjectMapper();
	}

}
