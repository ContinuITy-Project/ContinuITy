package org.continuity.cli.config;

import org.continuity.cli.storage.OrderStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Henning Schulz
 *
 */
@Configuration
public class OrderStorageConfig {

	@Bean
	public OrderStorage orderStorage() {
		return new OrderStorage();
	}

}
