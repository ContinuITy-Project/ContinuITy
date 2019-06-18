package org.continuity.idpa.config;

import org.continuity.idpa.storage.AnnotationStorageManager;
import org.continuity.idpa.storage.ApplicationStorageManager;
import org.continuity.idpa.storage.IdpaStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class StorageConfig {

	@Bean
	IdpaStorage idpaStorage(@Value("${storage.path:storage}") String storagePath) {
		return new IdpaStorage(storagePath);
	}

	@Bean
	ApplicationStorageManager applicationStorageManager(IdpaStorage repositoy) {
		return new ApplicationStorageManager(repositoy);
	}

	@Bean
	AnnotationStorageManager annotationStorageManager(IdpaStorage storage) {
		return new AnnotationStorageManager(storage);
	}

}
