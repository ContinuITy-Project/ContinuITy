package org.continuity.cobra.config;

import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.cobra.CobraConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationConfig {

	@Bean
	public ConfigurationProvider<CobraConfiguration> configurationHolder() {
		return new ConfigurationProvider<>(CobraConfiguration.class);
	}

}
