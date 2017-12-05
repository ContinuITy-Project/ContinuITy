package org.continuity.cli.commands;

import java.io.IOException;

import org.continuity.cli.config.PropertiesProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

/**
 * Overrides the default exit command and stores the properties before exiting.
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class CustomExitCommand implements Quit.Command {

	@Autowired
	private PropertiesProvider propertiesProvider;

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
