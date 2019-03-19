package org.continuity.cli.config;

import org.continuity.cli.manage.CliContextManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Henning Schulz
 *
 */
@Configuration
public class CliContextManagerConfig {

	@Bean
	public CliContextManager contextManager() {
		return new CliContextManager();
	}

}
