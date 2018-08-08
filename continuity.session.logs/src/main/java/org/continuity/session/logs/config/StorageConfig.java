package org.continuity.session.logs.config;

import java.nio.file.Paths;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.commons.storage.MixedStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MixedStorage<SessionLogs> sessionLogStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(Paths.get(storagePath), new SessionLogs());
	}

}
