package org.continuity.orchestrator.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Henning Schulz
 *
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.basePackage("org.continuity.orchestrator.controllers")).build().apiInfo(apiInfo());
	}

	private ApiInfo apiInfo() {
		return new ApiInfo("ContinuITy REST API", "Automated load testing in continuous software engineering.", "0.1.0", "Terms of service",
				new Contact("ContinuITy Project", "https://continuity-project.github.io/", ""), "Apache 2.0", "http://www.apache.org/licenses/LICENSE-2.0", Collections.emptyList());
	}

}
