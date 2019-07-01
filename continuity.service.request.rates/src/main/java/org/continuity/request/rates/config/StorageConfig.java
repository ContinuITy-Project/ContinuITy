package org.continuity.request.rates.config;

import java.nio.file.Paths;

import org.continuity.commons.storage.JsonFileStorage;
import org.continuity.commons.storage.MixedStorage;
import org.continuity.request.rates.model.RequestRatesModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MixedStorage<RequestRatesModel> wessbasStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(RequestRatesModel.class, new JsonFileStorage<RequestRatesModel>(Paths.get(storagePath), new RequestRatesModel()));
	}

}
