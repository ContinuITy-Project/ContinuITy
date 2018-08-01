package org.continuity.orchestrator.config;

import org.continuity.commons.storage.MemoryStorage;
import org.continuity.orchestrator.entities.Recipe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MemoryStorage<Recipe> recipeStorage() {
		return new MemoryStorage<>(Recipe.class);
	}

}
