package org.continuity.cli.commands;

import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
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
public class TagAndVersionCommands {

	@Autowired
	private CliContextManager contextManager;

	@ShellMethod(key = { "tag" }, value = "Sets the global tag to be used.")
	public String setTag(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) {
		if (Shorthand.DEFAULT_VALUE.equals(tag)) {
			return contextManager.getCurrentTag();
		} else {
			contextManager.setCurrentTag(tag);
			return null;
		}
	}

	@ShellMethod(key = { "tag reset" }, value = "Resets the global tag.")
	public void resetTag() {
		contextManager.setCurrentTag(null);
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
