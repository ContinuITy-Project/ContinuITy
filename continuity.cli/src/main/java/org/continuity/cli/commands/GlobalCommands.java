package org.continuity.cli.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.continuity.cli.config.PropertiesProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

/**
 * @author Henning Schulz
 *
 */
@ShellComponent
public class GlobalCommands {

	@Autowired
	private PropertiesProvider propertiesProvider;

	@ShellMethod(key = { "init", "load", "properties", "props" }, value = "Loads a properties file.")
	public String loadProperties(String path) throws FileNotFoundException, IOException {
		propertiesProvider.init(path);
		new File(propertiesProvider.get().getProperty(PropertiesProvider.KEY_WORKING_DIR)).mkdirs();
		return "Successfully loaded the properties file. Content is:\n" + propertiesProvider.get();
	}

	@ShellMethod(key = { "store-properties", "save-properties", "store-props", "save-props" }, value = "Stores the current properties to a file. Will be automatically done when exiting with \"exit\"")
	public String storeProperties() throws FileNotFoundException, IOException {
		propertiesProvider.save();
		return "Successfully stored the properties.";
	}

	@ShellMethod(key = { "working-directory", "wd" }, value = "Sets the working directory, where files are stored.")
	public String setWorkingDir(String path) {
		propertiesProvider.get().put(PropertiesProvider.KEY_WORKING_DIR, path);
		new File(path).mkdirs();
		return "Set working directory.";
	}

	@ShellMethod(key = { "url", "set-url" }, value = "Sets the URL where ContinuITy is running.")
	public String setUrl(String url) {
		String oldUrl = propertiesProvider.get().getProperty(PropertiesProvider.KEY_URL);
		propertiesProvider.get().put(PropertiesProvider.KEY_URL, url);
		return oldUrl == null ? "Set the new URL." : "Replaced " + oldUrl;
	}

}
