package org.continuity.idpa.application.config;

import org.continuity.idpa.application.repository.ApplicationModelRepository;
import org.continuity.idpa.application.repository.ApplicationModelRepositoryManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Henning Schulz
 *
 */
@Configuration
public class RepositoryConfig {

	@Bean
	ApplicationModelRepository systemModelRepository(@Value("${storage.path:storage}") String storagePath) {
		return new ApplicationModelRepository(storagePath);
	}

	@Bean
	ApplicationModelRepositoryManager systemModelRepositoryManager(ApplicationModelRepository repositoy) {
		return new ApplicationModelRepositoryManager(repositoy);
	}

}
