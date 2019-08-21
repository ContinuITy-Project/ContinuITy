package org.continuity.cobra.config;

import org.continuity.commons.idpa.AppIdConverter;
import org.continuity.commons.idpa.VersionOrTimestampConverter;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

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

	@Bean
	Module jdk8Module() {
		return new Jdk8Module();
	}

}
