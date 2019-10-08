package org.continuity.cli.commands;

import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@ShellCommandGroup("Shorthands (Idpa-Specific)")
public class IdpaSpecificShorthands {

	@Autowired
	private CliContextManager contextManager;

	@ShellMethod(key = { "app" }, value = "Shorthand for 'idpa app'.")
	@ShellMethodAvailability({ "appAvailability" })
	public AttributedString app(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("app");
		return shorthand.execute(unknown);
	}

	public Availability appAvailability() {
		return contextManager.getAvailablility("app");
	}

	@ShellMethod(key = { "app upload" }, value = "Shorthand for 'idpa app upload'.")
	@ShellMethodAvailability({ "appUploadAvailability" })
	public AttributedString uploadApplication(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String pattern) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("app upload");
		return shorthand.execute(pattern);
	}

	public Availability appUploadAvailability() {
		return contextManager.getAvailablility("app upload");
	}

	@ShellMethod(key = { "app init" }, value = "Shorthand for 'idpa app init'.")
	@ShellMethodAvailability({ "appInitAvailability" })
	public AttributedString initApplication(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("app init");
		return shorthand.execute(appId);
	}

	public Availability appInitAvailability() {
		return contextManager.getAvailablility("app init");
	}

	@ShellMethod(key = { "app create" }, value = "Shorthand for 'idpa app create'.")
	@ShellMethodAvailability({ "appCreateAvailability" })
	public AttributedString createIdpaApplication(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String openApiLocation,
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId)
			throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("app create");
		return shorthand.execute(openApiLocation, appId);
	}

	public Availability appCreateAvailability() {
		return contextManager.getAvailablility("app create");
	}

	@ShellMethod(key = { "app update" }, value = "Shorthand for 'idpa app update'.")
	@ShellMethodAvailability({ "appUpdateAvailability" })
	public AttributedString updateIdpaApplication(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String openApiLocation,
			@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = "false", value = { "--add", "-a" }, help = "Consider element additions.") boolean add,
			@ShellOption(defaultValue = "false", value = { "--remove", "-r" }, help = "Consider element removals.") boolean remove,
			@ShellOption(defaultValue = "false", value = { "--change", "-c" }, help = "Consider element changes.") boolean change,
			@ShellOption(defaultValue = "false", value = { "--endpoints", "-e" }, help = "Consider endpoints.") boolean endpoints,
			@ShellOption(defaultValue = "false", value = { "--parameters", "-p" }, help = "Consider parameters.") boolean parameters,
			@ShellOption(defaultValue = "false", value = { "--hide-ignored" }, help = "Consider parameters.") boolean hideIgnored) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("app update");
		return shorthand.execute(openApiLocation, appId, add, remove, change, endpoints, parameters, hideIgnored);
	}

	public Availability appUpdateAvailability() {
		return contextManager.getAvailablility("app update");
	}

	@ShellMethod(key = { "ann" }, value = "Shorthand for 'idpa ann'.")
	@ShellMethodAvailability({ "annAvailability" })
	public AttributedString ann(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE, help = "[for internal use]") String unknown) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann");
		return shorthand.execute(unknown);
	}

	public Availability annAvailability() {
		return contextManager.getAvailablility("ann");
	}

	@ShellMethod(key = { "ann upload" }, value = "Shorthand for 'idpa ann upload'.")
	@ShellMethodAvailability({ "annUploadAvailability" })
	public AttributedString uploadAnnotation(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String pattern) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann upload");
		return shorthand.execute(pattern);
	}

	public Availability annUploadAvailability() {
		return contextManager.getAvailablility("ann upload");
	}

	@ShellMethod(key = { "ann init" }, value = "Shorthand for 'idpa ann init'.")
	@ShellMethodAvailability({ "annInitAvailability" })
	public AttributedString initAnnotation(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann init");
		return shorthand.execute(appId);
	}

	public Availability annInitAvailability() {
		return contextManager.getAvailablility("ann init");
	}

	@ShellMethod(key = { "ann extract" }, value = "Shorthand for 'idpa ann extract'.")
	@ShellMethodAvailability({ "annExtractAvailability" })
	public AttributedString extractAnnotation(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String logsFile, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String regex) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann extract");
		return shorthand.execute(logsFile, appId, regex);
	}

	public Availability annExtractAvailability() {
		return contextManager.getAvailablility("ann extract");
	}

	@ShellMethod(key = { "ann check" }, value = "Shorthand for 'idpa ann check'.")
	@ShellMethodAvailability({ "annCheckAvailability" })
	public AttributedString checkAnnotation(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("ann check");
		return shorthand.execute(appId);
	}

	public Availability annCheckAvailability() {
		return contextManager.getAvailablility("ann check");
	}

}
