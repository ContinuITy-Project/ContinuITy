package org.continuity.jmeter.config;

import java.nio.file.Paths;

import org.continuity.api.entities.artifact.JMeterTestPlanBundle;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.commons.storage.MixedStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

	@Bean
	@Primary
	public MixedStorage<JMeterTestPlanBundle> testPlanStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(Paths.get(storagePath), new JMeterTestPlanBundle());
	}

	@Bean
	public MemoryStorage<String> reportStorage() {
		return new MemoryStorage<>(String.class);
	}

}
