package org.continuity.benchflow.config;

import org.continuity.commons.storage.MemoryStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import cloud.benchflow.dsl.definition.workload.HttpWorkload;

/**
 * 
 * @author Manuel Palenga
 *
 */
@Configuration
public class StorageConfig {

	@Bean
	@Primary
	public MemoryStorage<HttpWorkload> benchflowDSLStorage() {
		return new MemoryStorage<>(HttpWorkload.class);
	}

	@Bean
	public MemoryStorage<String> reportStorage() {
		return new MemoryStorage<>(String.class);
	}

}
