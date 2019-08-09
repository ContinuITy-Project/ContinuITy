package org.continuity.cli.commands;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.continuity.api.entities.config.MeasurementDataSpec;
import org.continuity.api.entities.exchange.MeasurementDataType;
import org.continuity.api.rest.RestApi;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.storage.IdpaStorage;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.accesslogs.UnifiedCsvFromAccessLogsExtractor;
import org.continuity.commons.utils.FileUtils;
import org.continuity.commons.utils.WebUtils;
import org.continuity.idpa.application.Application;
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
public class DataCommands extends AbstractCommands {

	private static final String CONTEXT_NAME = "data";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("unify", this, "createUnifiedCsv", String.class, String.class), //
			new Shorthand("upload", this, "upload", String.class, String.class, String.class, String.class, boolean.class) //
	);

	private final RestTemplate restTemplate;

	private final PropertiesProvider propertiesProvider;

	private final CliContextManager contextManager;

	private final IdpaStorage idpaStorage;

	@Autowired
	public DataCommands(CliContextManager contextManager, RestTemplate restTemplate, PropertiesProvider propertiesProvider, IdpaStorage idpaStorage) {
		super(contextManager);
		this.restTemplate = restTemplate;
		this.propertiesProvider = propertiesProvider;
		this.contextManager = contextManager;
		this.idpaStorage = idpaStorage;
	}

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
			@ShellOption(value = "--version", defaultValue = Shorthand.DEFAULT_VALUE) String passedVersion, @ShellOption(value = { "--finish", "-f" }, defaultValue = "false") boolean finish)
			throws Exception {

		return executeWithAppIdAndVersion(appId, passedVersion, (aid, version) -> {
			MeasurementDataType mType = MeasurementDataType.fromPrettyString(type);

			if (mType == null) {
				return new ResponseBuilder().error("Unknown measurement data type ").boldError(type).error("!").build();
			}

			String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
			String url = WebUtils.addProtocolIfMissing(propertiesProvider.getProperty(PropertiesProvider.KEY_URL));

			ResponseEntity<String> response;
			ResponseBuilder answer = new ResponseBuilder();

			if (path.startsWith("http")) {
				MeasurementDataSpec spec = new MeasurementDataSpec();
				spec.setLink(path);
				spec.setType(mType);

				try {
					response = restTemplate.postForEntity(
							RestApi.Cobra.MeasurementData.PUSH_LINK.viaOrchestrator().requestUrl(aid, version).withQuery("finish", Boolean.toString(finish)).withHost(url).get(), spec,
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
								RestApi.Cobra.MeasurementData.PUSH_FOR_TYPE.get(mType).viaOrchestrator().requestUrl(aid, version).withQuery("finish", Boolean.toString(finish)).withHost(url).get(),
								content, String.class);
					} catch (HttpStatusCodeException e) {
						response = new ResponseEntity<String>(e.getResponseBodyAsString(), e.getStatusCode());
					}

					appendAnswer(answer, mType, file.toString(), response, first);
					first = false;
				}
			}

			return answer.build();
		});
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
		answer.normal("Successfully uploaded the data of type ").bold(mType.toPrettyString()).normal(" at ").bold(path).newline()
				.normal("The data will now be processed and can be later retrieved at ").bold(response.getBody());
	}

	private void appendError(ResponseBuilder answer, MeasurementDataType mType, String path, ResponseEntity<String> response) {
		answer.error("Could not upload the specified data of type ").boldError(mType.toPrettyString()).error(" at ").boldError(path).error("! The response was ").boldError(response.getStatusCode())
				.error(" (").error(response.getStatusCode().getReasonPhrase()).error("): ").error(response.getBody());
	}

	@ShellMethod(key = { "data unify" }, value = "Creates a unified CSV from access logs holding the required information for session logs creation based on an application model.")
	public AttributedString createUnifiedCsv(String pathToAccessLogs, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Exception {
		return executeWithAppId(appId, aid -> {
			Application application = idpaStorage.readApplication(aid);

			String workingDir = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);
			Path accessLogsPath = Paths.get(workingDir).resolve(pathToAccessLogs);
			Path outputPath = accessLogsPath.getParent().resolve(accessLogsPath.getFileName() + "-unified.csv");

			UnifiedCsvFromAccessLogsExtractor extractor = new UnifiedCsvFromAccessLogsExtractor(application, accessLogsPath, outputPath);
			extractor.consume();

			String ignored = extractor.getIgnoredRequests().stream().collect(Collectors.joining("\n"));

			return new ResponseBuilder().normal("Created a unified CSV and stored it to ").normal(outputPath.toAbsolutePath())
					.normal("\nThe following requests have been ignored because the could not be mapped to an endpoint:\n").normal(ignored).build();
		});
	}

}
