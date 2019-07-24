package org.continuity.cobra.config;

import java.io.IOException;

import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ElasticsearchConfig {

	@Bean(destroyMethod = "destroy")
	public ElasticsearchTraceManager elasticsearchTraceManager(@Value("${elasticsearch.host:localhost}") String host, ObjectMapper mapper) throws IOException {
		return new ElasticsearchTraceManager(host, mapper);
	}

	@Bean(destroyMethod = "destroy")
	public ElasticsearchSessionManager elasticsearchSessionManager(@Value("${elasticsearch.host:localhost}") String host, ObjectMapper mapper) throws IOException {
		return new ElasticsearchSessionManager(host, mapper);
	}

}
