package org.continuity.cli.config;

import org.continuity.cli.storage.ConfigStorage;
import org.continuity.cli.storage.IdpaStorage;
import org.continuity.cli.storage.JMeterStorage;
import org.continuity.cli.storage.OrderStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Henning Schulz
 *
 */
@Configuration
public class StorageConfig {

	@Bean
	public OrderStorage orderStorage(PropertiesProvider properties, ObjectMapper mapper) {
		return new OrderStorage(properties, mapper);
	}

	@Bean
	public JMeterStorage jmeterStorage(PropertiesProvider properties) {
		return new JMeterStorage(properties);
	}

	@Bean
	public IdpaStorage idpaStorage(PropertiesProvider properties) {
		return new IdpaStorage(properties);
	}

	@Bean
	public ConfigStorage configStorage(PropertiesProvider properties) {
		return new ConfigStorage(properties);
	}

}
