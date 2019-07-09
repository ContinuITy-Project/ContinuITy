package org.continuity.orchestrator.config;

import java.nio.file.Paths;

import org.continuity.api.entities.deserialization.YamlJackson2HttpMessageConverter;
import org.continuity.commons.storage.MemoryStorage;
import org.continuity.orchestrator.entities.Recipe;
import org.continuity.orchestrator.storage.ConfigurationStorage;
import org.continuity.orchestrator.storage.TestingContextStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class StorageConfig {

	@Bean
	@Primary
	public MemoryStorage<Recipe> recipeStorage() {
		return new MemoryStorage<>(Recipe.class);
	}

	@Bean
	public TestingContextStorage testingContextStorage(@Value("${storage.path:storage}") String storagePath) {
		return new TestingContextStorage(Paths.get(storagePath));
	}

	@Bean
	public ConfigurationStorage configStorage(@Value("${config.storage.path:storage/config}") String storagePath) {
		return new ConfigurationStorage(Paths.get(storagePath), new YamlJackson2HttpMessageConverter().getObjectMapper());
	}

}
