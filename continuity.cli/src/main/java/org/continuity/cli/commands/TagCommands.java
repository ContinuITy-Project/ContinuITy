package org.continuity.cli.commands;

import org.continuity.cli.manage.CliContext;
import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

/**
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
public class TagCommands {

	private static final String CONTEXT_NAME = "tag";

	@Autowired
	private CliContextManager contextManager;

	private final CliContext context = new CliContext(CONTEXT_NAME, //
			new Shorthand("set", this, "setTag", String.class), //
			new Shorthand("reset", this, "resetTag") //
	);

	@ShellMethod(key = { CONTEXT_NAME }, value = "Goes to the 'tag' context so that the shorthands can be used.")
	public void goToTagContext() {
		contextManager.goToContext(context);
	}

	@ShellMethod(key = { "tag set" }, value = "Sets the global tag to be used.")
	public void setTag(String tag) {
		contextManager.setCurrentTag(tag);
	}

	@ShellMethod(key = { "tag reset" }, value = "Resets the global tag.")
	public void resetTag() {
		contextManager.setCurrentTag(null);
	}

}
