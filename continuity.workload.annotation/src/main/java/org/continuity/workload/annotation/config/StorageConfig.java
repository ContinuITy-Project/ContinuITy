package org.continuity.workload.annotation.config;

import org.continuity.workload.annotation.storage.AnnotationStorage;
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
	AnnotationStorage annotationStorage(@Value("${storage.path:/storage}") String storagePath) {
		return new AnnotationStorage(storagePath);
	}

}
