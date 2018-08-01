package org.continuity.jmeter.config;

import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.commons.storage.MemoryStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

	@Bean
	@Primary
	public MemoryStorage<JMeterTestPlanBundle> testPlanStorage() {
		return new MemoryStorage<>(JMeterTestPlanBundle.class);
	}

	@Bean
	public MemoryStorage<String> reportStorage() {
		return new MemoryStorage<>(String.class);
	}

}
