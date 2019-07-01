package org.continuity.session.logs.config;

import org.continuity.session.logs.managers.ElasticsearchManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ElasticsearchConfig {

	@Bean(destroyMethod = "destroy")
	public ElasticsearchManager elasticsearchManager(@Value("${elasticsearch.host:localhost}") String host, ObjectMapper mapper) {
		return new ElasticsearchManager(host, mapper);
	}

}
