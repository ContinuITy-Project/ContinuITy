package org.continuity.cli.commands;

import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.idpa.AppId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class AppIdAndVersionCommands {

	@Autowired
	private CliContextManager contextManager;

	@ShellMethod(key = { "app-id" }, value = "Sets the global app-id to be used.")
	public String setAppId(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String appId) {
		if (Shorthand.DEFAULT_VALUE.equals(appId)) {
			return contextManager.getCurrentAppId().toString();
		} else {
			contextManager.setCurrentAppId(AppId.fromString(appId));
			return null;
		}
	}

	@ShellMethod(key = { "app-id reset" }, value = "Resets the global app-id.")
	public void resetAppId() {
		contextManager.setCurrentAppId(null);
	}

	@ShellMethod(key = { "version" }, value = "Sets the global version or timestamp to be used.")
	public String setVersion(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String version) {
		if (Shorthand.DEFAULT_VALUE.equals(version)) {
			return contextManager.getCurrentVersion();
		} else {
			contextManager.setCurrentVersion(version);
			return null;
		}
	}

	@ShellMethod(key = { "version reset" }, value = "Resets the global version or timestamp.")
	public void resetVersion() {
		contextManager.setCurrentVersion(null);
	}

}
