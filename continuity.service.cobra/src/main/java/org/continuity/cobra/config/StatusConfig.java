package org.continuity.cobra.config;

import org.continuity.cobra.entities.TraceProcessingStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatusConfig {

	@Bean
	public TraceProcessingStatus status() {
		return new TraceProcessingStatus();
	}

}
