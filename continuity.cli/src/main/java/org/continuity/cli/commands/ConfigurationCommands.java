package org.continuity.cli.commands;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.Collections;

import org.continuity.api.rest.RestApi;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.storage.ConfigStorage;
import org.continuity.cli.utils.ResponseBuilder;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Management of the ContinuITy configurations.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class ConfigurationCommands extends AbstractCommands {

	private static final String CONTEXT_NAME = "config";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("download", this, "downloadConfig", String.class, String.class), //
			new Shorthand("upload", this, "uploadConfig", String.class, String.class) //
	);

	private final PropertiesProvider propertiesProvider;

	private final RestTemplate restTemplate;

	private final CliContextManager contextManager;

	private final ConfigStorage storage;

	@Autowired
	public ConfigurationCommands(PropertiesProvider propertiesProvider, RestTemplate restTemplate, CliContextManager contextManager, ConfigStorage storage) {
		super(contextManager);
		this.propertiesProvider = propertiesProvider;
		this.restTemplate = restTemplate;
		this.contextManager = contextManager;
		this.storage = storage;
	}

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'config' context so that the shorthands can be used.")
	public AttributedString goToConfigContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "config download" }, value = "Downloads and opens the configuration for the specified ContinuITy service and app-id.")
	public AttributedString downloadConfig(String service, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.valueOf("application/x-yaml")));
			HttpEntity<String> entity = new HttpEntity<>(headers);

			String host = propertiesProvider.getProperty(PropertiesProvider.KEY_URL);

			ResponseEntity<String> response;
			try {
				response = restTemplate.exchange(RestApi.Orchestrator.Configuration.GET.requestUrl(service, aid).withHost(host).withQuery("init", "true").get(), HttpMethod.GET, entity, String.class);
			} catch (HttpStatusCodeException e) {
				response = ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
			}

			ResponseBuilder builder = new ResponseBuilder();

			if (response.getStatusCode().is4xxClientError()) {
				builder.error("There is no such configuration stored! Got the default one.").newline();
			}

			if (response.getStatusCode().is5xxServerError()) {
				return new ResponseBuilder().error("Error response from ContinuITy!").appendStatusCode(response.getStatusCode()).build();
			} else {
				Path path = storage.store(response.getBody(), service, aid);
				Desktop.getDesktop().open(path.toFile());

				return builder.normal("Downloaded and opened the configuration. Local path: ").normal(path).build();
			}
		});
	}

	@ShellMethod(key = { "config upload" }, value = "Uploads the configuration for the specified ContinuITy service and app-id.")
	public AttributedString uploadConfig(String service, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, (aid) -> {
			String config = storage.read(service, aid);

			if (config == null) {
				return new ResponseBuilder().error("There is no such configuration stored locally! Please download it using ").boldError("config download").error("?").build();
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.valueOf("application/x-yaml"));
			HttpEntity<String> entity = new HttpEntity<String>(config, headers);

			String host = propertiesProvider.getProperty(PropertiesProvider.KEY_URL);

			ResponseEntity<String> response;
			try {
				response = restTemplate.exchange(RestApi.Orchestrator.Configuration.POST.requestUrl().withHost(host).get(), HttpMethod.POST, entity, String.class);
			} catch (HttpStatusCodeException e) {
				response = ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
			}

			if (response.getStatusCode().is4xxClientError()) {
				return new ResponseBuilder().error("Error response from ContinuITy!").appendStatusCode(response.getStatusCode()).error("Is the configuration properly specified?").build();
			} else if (response.getStatusCode().is2xxSuccessful()) {
				return new ResponseBuilder().normal("Upload successful. Response from server: ").normal(response.getBody()).build();
			} else {
				return new ResponseBuilder().error("Error response from ContinuITy!").appendStatusCode(response.getStatusCode()).build();
			}
		});
	}

}
