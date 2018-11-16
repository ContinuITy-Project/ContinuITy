package org.continuity.wessbas.config;

import java.nio.file.Paths;

import org.continuity.commons.storage.MixedStorage;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.storage.WessbasFileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

	@Bean
	public MixedStorage<WessbasBundle> wessbasStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(WessbasBundle.class, new WessbasFileStorage(Paths.get(storagePath)));
	}
	
	@Bean
	public MixedStorage<BehaviorModelPack> behaviorModelStorage(@Value("${storage.path:storage}") String storagePath) {
		return new MixedStorage<>(Paths.get(storagePath), new BehaviorModelPack());
	}

}
