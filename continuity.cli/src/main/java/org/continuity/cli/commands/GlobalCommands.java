package org.continuity.cli.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.cli.config.PropertiesProvider;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
public class GlobalCommands implements Quit.Command {

	private static final String DEFAULT_VALUE = "$DEFAULT$";

	@Autowired
	private PropertiesProvider propertiesProvider;

	@Autowired
	private CliContextManager contextManager;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ObjectMapper mapper;

	@ShellMethod(key = { "props" }, value = "Loads a properties file or shows the current content.")
	public String loadProperties(@ShellOption(help = "If not set, the current content is printed.", defaultValue = DEFAULT_VALUE) String path) throws FileNotFoundException, IOException {
		if (DEFAULT_VALUE.equals(path)) {
			return "Properties at " + propertiesProvider.getPath() + ":\n" + propertiesProvider.get().toString();
		} else {
			propertiesProvider.init(path);
			new File(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR)).mkdirs();
			return "Successfully loaded the properties file. Content is:\n" + propertiesProvider.get();
		}
	}

	@ShellMethod(key = { "props-store", "props-save" }, value = "Stores the current properties to a file. Will be automatically done when exiting with \"exit\"")
	public String storeProperties() throws FileNotFoundException, IOException {
		propertiesProvider.save();
		return "Successfully stored the properties.";
	}

	@ShellMethod(key = { "working-directory", "wd" }, value = "Sets the working directory, where files are stored.")
	public String setWorkingDir(@ShellOption(help = "If not set, the current working directory is printed.", defaultValue = DEFAULT_VALUE) String path) {
		String currWd = propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR);

		if (DEFAULT_VALUE.equals(path)) {
			return "Current working directory is " + currWd;
		} else {
			propertiesProvider.get().put(PropertiesProvider.KEY_WORKING_DIR, path);
			new File(path).mkdirs();
			return "Set working directory. Old one was " + currWd;
		}
	}

	@ShellMethod(key = { "url" }, value = "Sets the URL where ContinuITy is running.")
	public String setUrl(@ShellOption(help = "If not set, the current url is printed.", defaultValue = DEFAULT_VALUE) String url) {
		String currUrl = propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL);

		if (DEFAULT_VALUE.equals(url)) {
			return "Current url is " + currUrl;
		} else {
			propertiesProvider.get().put(PropertiesProvider.KEY_URL, url);
			return currUrl == null ? "Set the new URL." : "Replaced " + currUrl;
		}
	}

	@ShellMethod(key = { "get" }, value = "Gets an artifact.")
	public String get(String link) throws JsonProcessingException {
		ResponseEntity<JsonNode> response = restTemplate.getForEntity(link, JsonNode.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			return "Could not access the link! Response code is " + response.getStatusCode();
		}

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getBody());
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
