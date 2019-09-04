package org.continuity.cobra.config;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.continuity.api.entities.artifact.ForecastIntensityRecord;
import org.continuity.commons.storage.MixedStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MixedStorage<List<ForecastIntensityRecord>> intensityStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(Paths.get(storagePath), new ArrayList<>());
	}

}
