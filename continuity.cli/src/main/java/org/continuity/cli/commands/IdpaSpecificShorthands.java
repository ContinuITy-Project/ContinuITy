package org.continuity.cli.commands;

import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@ShellCommandGroup("Idpa-Specific Shorthands")
public class IdpaSpecificShorthands {

	@Autowired
	private CliContextManager contextManager;

	@ShellMethod(key = { "app" }, value = "Shorthand for 'idpa app'.")
	@ShellMethodAvailability({ "appAvailability" })
	public void app() throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("app");
		shorthand.execute();
	}

	public Availability appAvailability() {
		return contextManager.getAvailablility("app");
	}

	@ShellMethod(key = { "app upload" }, value = "Shorthand for 'idpa app upload'.")
	@ShellMethodAvailability({ "appUploadAvailability" })
	public String uploadApplication(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String pattern) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("app upload");
		String defaultOrActual = shorthand.checkRequiredParameter(pattern, 0, "--pattern");
		return shorthand.execute(defaultOrActual);
	}

	public Availability appUploadAvailability() {
		return contextManager.getAvailablility("app upload");
	}

	@ShellMethod(key = { "ann" }, value = "Shorthand for 'idpa ann'.")
	@ShellMethodAvailability({ "annAvailability" })
	public void ann() throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann");
		shorthand.execute();
	}

	public Availability annAvailability() {
		return contextManager.getAvailablility("ann");
	}

	@ShellMethod(key = { "ann upload" }, value = "Shorthand for 'idpa ann upload'.")
	@ShellMethodAvailability({ "annUploadAvailability" })
	public String uploadAnnotation(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String pattern) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann upload");
		String defaultOrActual = shorthand.checkRequiredParameter(pattern, 0, "--pattern");
		return shorthand.execute(defaultOrActual);
	}

	public Availability annUploadAvailability() {
		return contextManager.getAvailablility("ann upload");
	}

	@ShellMethod(key = { "ann init" }, value = "Shorthand for 'idpa ann init'.")
	@ShellMethodAvailability({ "annInitAvailability" })
	public String initAnnotation(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann init");
		String defaultOrActual = shorthand.checkRequiredParameter(tag, 0, "--tag");
		return shorthand.execute(defaultOrActual);
	}

	public Availability annInitAvailability() {
		return contextManager.getAvailablility("ann init");
	}

}
