package org.continuity.cli.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.continuity.api.entities.config.MeasurementDataSpec;
import org.continuity.api.entities.links.MeasurementDataType;
import org.continuity.api.rest.RestApi;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.accesslogs.UnifiedCsvFromAccessLogsExtractor;
import org.continuity.commons.utils.FileUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.AppId;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Commands for dealing with Access logs.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class DataCommands {

	private static final String CONTEXT_NAME = "data";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("unify", this, "createUnifiedCsv", String.class, String.class), //
			new Shorthand("upload", this, "upload", String.class, String.class, String.class, String.class, boolean.class) //
	);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private CliContextManager contextManager;

	private final IdpaYamlSerializer<Application> appSerializer = new IdpaYamlSerializer<>(Application.class);

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'accesslogs' context so that the shorthands can be used.")
	public AttributedString goToAccesslogsContext(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) {
		if (Shorthand.DEFAULT_VALUE.equals(unknown)) {
			contextManager.goToContext(context);
			return null;
		} else {
			return new ResponseBuilder().error("Unknown sub command ").boldError(unknown).error("!").build();
		}
	}

	@ShellMethod(key = { "data upload" }, value = "Uploads data of a certain type (open-xtrace, access-logs, csv) for later use.")
	public AttributedString upload(@ShellOption(help = "Location where the data can be found. Can be a URL or a file path. A file name can contain UNIX-like wildcards.") String path,
			@ShellOption(defaultValue = "open-xtrace") String type, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String version, @ShellOption(value = { "--finish", "-f" }, defaultValue = "false") boolean finish) throws IOException {
		MeasurementDataType mType = MeasurementDataType.fromPrettyString(type);

		if (mType == null) {
			return new ResponseBuilder().error("Unknown measurement data type ").boldError(type).error("!").build();
		}

		AppId aid = contextManager.getAppIdOrFail(appId);
		version = contextManager.getVersionOrFail(version);

		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));

		ResponseEntity<String> response;
		ResponseBuilder answer = new ResponseBuilder();

		if (path.startsWith("http")) {
			MeasurementDataSpec spec = new MeasurementDataSpec();
			spec.setLink(path);
			spec.setType(mType);

			try {
				response = restTemplate.postForEntity(RestApi.Orchestrator.MeasurementData.PUSH_LINK.requestUrl(aid, version).withQuery("finish", Boolean.toString(finish)).withHost(url).get(), spec,
						String.class);
			} catch (HttpStatusCodeException e) {
				response = new ResponseEntity<String>(e.getResponseBodyAsString(), e.getStatusCode());
			}

			appendAnswer(answer, mType, path, response, true);
		} else {
			boolean first = true;

			for (File file : FileUtils.getAllFilesMatchingWildcards(Paths.get(workingDir).resolve(path))) {
				String content = new String(Files.readAllBytes(file.toPath()));

				try {
					response = restTemplate.postForEntity(
							RestApi.Orchestrator.MeasurementData.PUSH_FOR_TYPE.get(mType).requestUrl(aid, version).withQuery("finish", Boolean.toString(finish)).withHost(url).get(), content,
							String.class);
				} catch (HttpStatusCodeException e) {
					response = new ResponseEntity<String>(e.getResponseBodyAsString(), e.getStatusCode());
				}

				appendAnswer(answer, mType, file.toString(), response, first);
				first = false;
			}
		}

		return answer.build();
	}

	private void appendAnswer(ResponseBuilder answer, MeasurementDataType mType, String path, ResponseEntity<String> response, boolean first) {
		if (response.getStatusCode().is2xxSuccessful()) {
			appendSuccess(answer, mType, path, response);
		} else {
			appendError(answer, mType, path, response);
		}

		if (!first) {
			answer.newline();
		}
	}

	private void appendSuccess(ResponseBuilder answer, MeasurementDataType mType, String path, ResponseEntity<String> response) {
		answer.normal("Successfully uploaded the data of type ").bold(mType.toPrettyString()).normal(" at ").bold(path).newline().normal("The data can be retrieved at ").bold(response.getBody());
	}

	private void appendError(ResponseBuilder answer, MeasurementDataType mType, String path, ResponseEntity<String> response) {
		answer.error("Could not upload the specified data of type ").boldError(mType.toPrettyString()).error(" at ").boldError(path).error("! The response was ").boldError(response.getStatusCode())
				.error(" (").error(response.getStatusCode().getReasonPhrase()).error("): ").error(response.getBody());
	}

	@ShellMethod(key = { "data unify" }, value = "Creates a unified CSV from access logs holding the required information for session logs creation based on an application model.")
	public String createUnifiedCsv(String pathToAccessLogs, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws IOException {
		AppId aid = contextManager.getAppIdOrFail(appId);

		Application application = readApplicationModel(aid);

		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		Path accessLogsPath = Paths.get(workingDir).resolve(pathToAccessLogs);
		Path outputPath = accessLogsPath.getParent().resolve(accessLogsPath.getFileName() + "-unified.csv");

		UnifiedCsvFromAccessLogsExtractor extractor = new UnifiedCsvFromAccessLogsExtractor(application, accessLogsPath, outputPath);
		extractor.consume();

		String ignored = extractor.getIgnoredRequests().stream().collect(Collectors.joining("\n"));

		return new StringBuilder().append("Created a unified CSV and stored it to ").append(outputPath.toAbsolutePath())
				.append("\nThe following requests have been ignored because the could not be mapped to an endpoint:\n").append(ignored).toString();
	}

	private Application readApplicationModel(AppId aid) throws IOException {
		String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + aid + ".yml");

		if (applicationFile.exists()) {
			return appSerializer.readFromYaml(applicationFile);
		} else {
			return null;
		}
	}

}
