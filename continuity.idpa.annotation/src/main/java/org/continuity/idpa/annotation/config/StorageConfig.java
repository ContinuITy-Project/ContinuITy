package org.continuity.idpa.annotation.config;

import org.continuity.idpa.annotation.storage.AnnotationStorage;
import org.continuity.idpa.annotation.storage.AnnotationStorageManager;
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
	AnnotationStorage annotationStorage(@Value("${storage.path:storage}") String storagePath) {
		return new AnnotationStorage(storagePath);
	}

	@Bean
	AnnotationStorageManager annotationStorageManager(AnnotationStorage storage) {
		return new AnnotationStorageManager(storage);
	}

}
