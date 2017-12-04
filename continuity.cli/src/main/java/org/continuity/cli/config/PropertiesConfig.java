package org.continuity.cli.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class PropertiesConfig {

	@Bean
	public PropertiesProvider properties() {
		return new PropertiesProvider();
	}

}
