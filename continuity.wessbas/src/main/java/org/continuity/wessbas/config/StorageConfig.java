package org.continuity.wessbas.config;

import org.continuity.commons.storage.MemoryStorage;
import org.continuity.wessbas.entities.WessbasBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MemoryStorage<WessbasBundle> sessionLogStorage() {
		return new MemoryStorage<>(WessbasBundle.class);
	}

}
