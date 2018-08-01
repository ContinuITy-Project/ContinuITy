package org.continuity.session.logs.config;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.commons.storage.MemoryStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MemoryStorage<SessionLogs> sessionLogStorage() {
		return new MemoryStorage<>(SessionLogs.class);
	}

}
