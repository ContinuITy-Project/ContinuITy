package org.continuity.cli.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.continuity.cli.config.PropertiesProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
public class GlobalCommands {

	private static final String DEFAULT_VALUE = "$DEFAULT$";

	@Autowired
	private PropertiesProvider propertiesProvider;

	@ShellMethod(key = { "properties", "props" }, value = "Loads a properties file or shows the current content.")
	public String loadProperties(@ShellOption(help = "If not set, the current content is printed.", defaultValue = DEFAULT_VALUE) String path) throws FileNotFoundException, IOException {
		if (DEFAULT_VALUE.equals(path)) {
			return "Properties at " + propertiesProvider.getPath() + ":\n" + propertiesProvider.get().toString();
		} else {
			propertiesProvider.init(path);
			new File(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR)).mkdirs();
			return "Successfully loaded the properties file. Content is:\n" + propertiesProvider.get();
		}
	}

	@ShellMethod(key = { "store-properties", "save-properties", "store-props", "save-props" }, value = "Stores the current properties to a file. Will be automatically done when exiting with \"exit\"")
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

}
