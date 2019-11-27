package org.continuity.cobra.config;

import java.io.IOException;

import org.continuity.cobra.managers.ElasticsearchBehaviorManager;
import org.continuity.cobra.managers.ElasticsearchIntensityManager;
import org.continuity.cobra.managers.ElasticsearchSessionManager;
import org.continuity.cobra.managers.ElasticsearchTraceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ElasticsearchConfig {

	@Bean(destroyMethod = "destroy")
	public ElasticsearchTraceManager elasticsearchTraceManager(@Value("${elasticsearch.host:localhost}") String host, ObjectMapper mapper,
			@Value("${elasticsearch.bulk-timeout:30}") int bulkTimeoutSeconds) throws IOException {
		return new ElasticsearchTraceManager(host, mapper, bulkTimeoutSeconds);
	}

	@Bean(destroyMethod = "destroy")
	public ElasticsearchSessionManager elasticsearchSessionManager(@Value("${elasticsearch.host:localhost}") String host, ObjectMapper mapper,
			@Value("${elasticsearch.bulk-timeout:30}") int bulkTimeoutSeconds) throws IOException {
		return new ElasticsearchSessionManager(host, mapper, bulkTimeoutSeconds);
	}

	@Bean(destroyMethod = "destroy")
	public ElasticsearchIntensityManager elasticsearchIntensityManager(@Value("${elasticsearch.host:localhost}") String host, ObjectMapper mapper,
			@Value("${elasticsearch.bulk-timeout:30}") int bulkTimeoutSeconds) throws IOException {
		return new ElasticsearchIntensityManager(host, mapper, bulkTimeoutSeconds);
	}

	@Bean(destroyMethod = "destroy")
	public ElasticsearchBehaviorManager elasticsearchBehaviorManager(@Value("${elasticsearch.host:localhost}") String host, ObjectMapper mapper,
			@Value("${elasticsearch.bulk-timeout:30}") int bulkTimeoutSeconds) throws IOException {
		return new ElasticsearchBehaviorManager(host, mapper, bulkTimeoutSeconds);
	}

}
