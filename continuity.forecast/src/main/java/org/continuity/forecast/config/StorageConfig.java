package org.continuity.forecast.config;

import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.util.Pair;
import org.continuity.api.entities.artifact.ForecastBundle;
import org.continuity.commons.storage.MixedStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MixedStorage<ForecastBundle> forecastStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(Paths.get(storagePath), new ForecastBundle());
	}
	
	@Bean
	public ConcurrentHashMap<String, Pair<Date, Integer>> dateStorage() {
		return new ConcurrentHashMap<String, Pair<Date, Integer>>();
	}

}
