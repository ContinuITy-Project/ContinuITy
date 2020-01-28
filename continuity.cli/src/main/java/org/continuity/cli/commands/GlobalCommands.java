package org.continuity.cli.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.entities.exchange.ArtifactType;
import org.continuity.api.entities.report.OrderReport;
import org.continuity.api.rest.RestEndpoint;
import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.storage.OrderStorage;
import org.continuity.cli.utils.ResponseBuilder;
import org.continuity.idpa.AppId;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.standard.commands.Quit;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
public class GlobalCommands extends AbstractCommands implements Quit.Command {

	private static final String DEFAULT_VALUE = "$DEFAULT$";

	private final PropertiesProvider propertiesProvider;

	private final CliContextManager contextManager;

	private final RestTemplate restTemplate;

	private final ObjectMapper mapper;

	private final OrderStorage orderStorage;

	@Autowired
	public GlobalCommands(CliContextManager contextManager, PropertiesProvider propertiesProvider, RestTemplate restTemplate, ObjectMapper mapper, OrderStorage orderStorage) {
		super(contextManager);
		this.propertiesProvider = propertiesProvider;
		this.contextManager = contextManager;
		this.restTemplate = restTemplate;
		this.mapper = mapper;
		this.orderStorage = orderStorage;
	}

	@ShellMethod(key = { "props" }, value = "Shows the content of the current properties.")
	public String loadProperties() throws FileNotFoundException, IOException {
		return "Properties at " + propertiesProvider.getPath() + ":\n" + propertiesProvider.toString();
	}

	@ShellMethod(key = { "wd" }, value = "Sets the working directory, where files are stored.")
	public String setWorkingDir(@ShellOption(help = "If not set, the current working directory is printed.", defaultValue = DEFAULT_VALUE) String path) {
		String currWd = propertiesProvider.getProperty(PropertiesProvider.KEY_WORKING_DIR);

		if (DEFAULT_VALUE.equals(path)) {
			return "Current working directory is " + currWd;
		} else {
			propertiesProvider.putProperty(PropertiesProvider.KEY_WORKING_DIR, path);
			new File(path).mkdirs();
			return "Set working directory. Old one was " + currWd;
		}
	}

	@ShellMethod(key = { "url" }, value = "Sets the URL where ContinuITy is running.")
	public String setUrl(@ShellOption(help = "If not set, the current url is printed.", defaultValue = DEFAULT_VALUE) String url) {
		String currUrl = propertiesProvider.getProperty(PropertiesProvider.KEY_URL);

		if (DEFAULT_VALUE.equals(url)) {
			return "Current url is " + currUrl;
		} else {
			propertiesProvider.putProperty(PropertiesProvider.KEY_URL, url);
			return currUrl == null ? "Set the new URL." : "Replaced " + currUrl;
		}
	}

	@ShellMethod(key = { "get" }, value = "Gets an artifact.")
	public AttributedString get(@ShellOption(help = "Link as contained in an order report or field of the latest report.") String link,
			@ShellOption(help = "Indicates to store the retrieved artifact to the latest order's folder.") boolean store,
			@ShellOption(help = "Indicates not to print the retrieved artifact to console.") boolean silent) throws Exception {
		return execute(() -> {
			String linkToArtifact = link;

			if (!link.contains("/")) {
				AppId aid = contextManager.getCurrentAppId();

				if (aid == null) {
					return new ResponseBuilder().error("Missing an app-id! Please specify one using ").boldError("app-id <your_id>").error("!").build();
				}

				OrderReport report = orderStorage.readLatestReport(aid);

				if (report == null) {
					return new ResponseBuilder().error("Missing an order report! Please execute one first and get its result.").build();
				}

				String[] jsonPath = link.split("\\.");
				ArtifactType type = ArtifactType.fromPrettyString(jsonPath[0]);

				if (type == null) {
					return new ResponseBuilder().error("Unknown artifact name: ").boldError(jsonPath[0]).build();
				}

				if (jsonPath.length == 1) {
					linkToArtifact = type.getFromModel(report.getArtifacts()).getDefaultLink();
				} else {
					linkToArtifact = type.getFromModel(report.getArtifacts()).getLink(jsonPath[1]);
				}

				if (linkToArtifact == null) {
					return new ResponseBuilder().error("There is no artifact present at ").boldError(link).build();
				}
			}

			ResponseEntity<JsonNode> response = restTemplate.getForEntity(RestEndpoint.urlViaOrchestrator(linkToArtifact, propertiesProvider.getProperty(PropertiesProvider.KEY_URL)), JsonNode.class);

			if (store) {
				AppId aid = contextManager.getCurrentAppId();

				if (aid == null) {
					return new ResponseBuilder().error("Missing an app-id! Please specify one using ").boldError("app-id <your_id>").error("!").build();
				}

				orderStorage.storeArtifact(aid, linkToArtifact, response.getBody());
			}

			return silent ? null : new ResponseBuilder().normal(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody())).build();
		});
	}

	@ShellMethod(key = { ".." }, value = "Goes one context up.")
	public void goUp() {
		contextManager.removeTopmostContext();
	}

	@ShellMethod(key = { "shorthands" }, value = "Lists all available shorthands.")
	public AttributedString listShorthands() {
		List<Shorthand> shorthands = new ArrayList<>();
		shorthands.addAll(contextManager.getAllAvailableShorthands().values());
		Collections.sort(shorthands);

		AttributedString description;

		if (shorthands.isEmpty()) {
			description = new AttributedString("(none)");
		} else {
			description = AttributedString.join(new AttributedString("\n        "), shorthands.stream().map(this::formatShorthand).collect(Collectors.toList()));
		}

		return AttributedString.join(new AttributedString("\n        "), new AttributedString("Available Shorthands", AttributedStyle.BOLD), description);
	}

	private AttributedString formatShorthand(Shorthand shorthand) {
		return AttributedString.join(new AttributedString(": "), new AttributedString(shorthand.getShorthandName(), AttributedStyle.BOLD),
				new AttributedString("Shorthand for '" + shorthand.getCommandName() + "'"));
	}

	/**
	 * Overrides the default exit command and stores the properties before exiting.
	 */
	@ShellMethod(value = "Exit the shell.", key = { "quit", "exit" })
	public void quit() {
		if (propertiesProvider.isInitialized()) {
			try {
				propertiesProvider.save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		throw new ExitRequest();
	}

}
