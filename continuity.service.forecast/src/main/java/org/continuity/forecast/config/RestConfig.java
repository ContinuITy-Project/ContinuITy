package org.continuity.forecast.config;

import org.continuity.commons.idpa.AppIdConverter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
	Module jdk8Module() {
		return new Jdk8Module();
	}

	@Bean
	Module javaTimeModule() {
		return new JavaTimeModule();
	}

	@Bean
	Jackson2ObjectMapperBuilderCustomizer addCustomBigDecimalDeserialization() {
		return new Jackson2ObjectMapperBuilderCustomizer() {

			@Override
			public void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder) {
				jacksonObjectMapperBuilder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			}
		};
	}

}
