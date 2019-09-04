package org.continuity.cli.config;

import org.continuity.idpa.serialization.IdpaSerializationUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
		return IdpaSerializationUtils.getDefaultYamlObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	@Bean
	ObjectMapper jsonObjectMapper() {
		return IdpaSerializationUtils.getDefaultJsonObjectMapper().registerModule(new Jdk8Module()).registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

}
