package org.continuity.session.logs.config;

import org.continuity.api.entities.config.ConfigurationProvider;
import org.continuity.api.entities.config.session.logs.SessionLogsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigurationConfig {

	@Bean
	public ConfigurationProvider<SessionLogsConfiguration> configurationHolder() {
		return new ConfigurationProvider<>(SessionLogsConfiguration.class);
	}

}
