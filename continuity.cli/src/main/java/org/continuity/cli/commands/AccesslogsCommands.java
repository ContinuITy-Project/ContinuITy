package org.continuity.cli.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.commons.accesslogs.UnifiedCsvFromAccessLogsExtractor;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * Commands for dealing with Access logs.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class AccesslogsCommands {

	private static final String CONTEXT_NAME = "accesslogs";

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("unify", this, "createUnifiedCsv", String.class, String.class) //
	);

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

	@ShellMethod(key = { "accesslogs unify" }, value = "Creates a unified CSV holding the required information for session logs creation based on an application model.")
	public String createUnifiedCsv(String pathToAccessLogs, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws IOException {
		tag = contextManager.getTagOrFail(tag);

		Application application = readApplicationModel(tag);

		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		Path accessLogsPath = Paths.get(workingDir).resolve(pathToAccessLogs);
		Path outputPath = accessLogsPath.getParent().resolve(accessLogsPath.getFileName() + "-unified.csv");

		UnifiedCsvFromAccessLogsExtractor extractor = new UnifiedCsvFromAccessLogsExtractor(application, accessLogsPath, outputPath);
		extractor.consume();

		String ignored = extractor.getIgnoredRequests().stream().collect(Collectors.joining("\n"));

		return new StringBuilder().append("Created a unified CSV and stored it to ").append(outputPath.toAbsolutePath())
				.append("\nThe following requests have been ignored because the could not be mapped to an endpoint:\n").append(ignored).toString();
	}

	private Application readApplicationModel(String tag) throws IOException {
		String workingDir = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);
		File applicationFile = new File(workingDir + "/application-" + tag + ".yml");

		if (applicationFile.exists()) {
			return appSerializer.readFromYaml(applicationFile);
		} else {
			return null;
		}
	}

}
