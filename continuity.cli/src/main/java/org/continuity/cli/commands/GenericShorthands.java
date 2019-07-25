package org.continuity.cli.commands;

import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.continuity.cli.storage.OrderStorage;
import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

/**
 *
 * @author Henning Schulz
 *
 */
@ShellComponent
@ShellCommandGroup("Shorthands (Generic)")
public class GenericShorthands {

	@Autowired
	private CliContextManager contextManager;

	@ShellMethod(key = { "download" }, value = "Shorthand for '<context> download'. Available in 'idpa' and 'jmeter'.")
	@ShellMethodAvailability({ "downloadAvailability" })
	public AttributedString download(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String link, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String arg2) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("download");
		return shorthand.execute(link, arg2);
	}

	public Availability downloadAvailability() {
		return contextManager.getAvailablility("download");
	}

	@ShellMethod(key = { "upload" }, value = "Shorthand for '<context> upload'. Available in 'idpa app', 'idpa ann', and 'jmeter'.")
	@ShellMethodAvailability({ "uploadAvailability" })
	public AttributedString upload(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String arg1, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String arg2,
			@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String arg3, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String arg4,
			@ShellOption(value = { "--annotate", "-a" }, defaultValue = "false") boolean annotate, @ShellOption(value = { "--finish", "-f" }, defaultValue = "false") boolean finish) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("upload");

		// Quick fix due to limitations of Spring Shell
		if (shorthand.getCommandName().startsWith("data")) {
			return shorthand.execute(arg1, arg2, arg3, arg4, finish);
		} else {
			return shorthand.execute(arg1, arg2, annotate);
		}
	}

	public Availability uploadAvailability() {
		return contextManager.getAvailablility("upload");
	}

	@ShellMethod(key = { "init" }, value = "Shorthand for '<context> init'. Available in 'idpa ann'.")
	@ShellMethodAvailability({ "initAvailability" })
	public AttributedString init(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("init");
		return shorthand.execute(appId);
	}

	public Availability initAvailability() {
		return contextManager.getAvailablility("init");
	}

	@ShellMethod(key = { "extract" }, value = "Shorthand for '<context> extract'. Available in 'idpa ann'.")
	@ShellMethodAvailability({ "extractAvailability" })
	public AttributedString extract(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String logsFile, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String regex) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("extract");
		return shorthand.execute(logsFile, appId, regex);
	}

	public Availability extractAvailability() {
		return contextManager.getAvailablility("extract");
	}

	@ShellMethod(key = { "open" }, value = "Shorthand for '<context> open'. Available in 'idpa' and 'order'.")
	@ShellMethodAvailability({ "openAvailability" })
	public AttributedString open(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("open");
		return shorthand.execute(id);
	}

	public Availability openAvailability() {
		return contextManager.getAvailablility("open");
	}

	@ShellMethod(key = { "edit" }, value = "Shorthand for '<context> edit'. Available in 'order'.")
	@ShellMethodAvailability({ "editAvailability" })
	public AttributedString editOrder(@ShellOption(defaultValue = OrderStorage.ID_LATEST) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("open");
		return shorthand.execute(id);
	}

	public Availability editAvailability() {
		return contextManager.getAvailablility("edit");
	}

	@ShellMethod(key = { "home" }, value = "Shorthand for '<context> home'. Available in 'jmeter'.")
	@ShellMethodAvailability({ "homeAvailability" })
	public AttributedString home(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String path) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("home");
		return shorthand.execute(path);
	}

	public Availability homeAvailability() {
		return contextManager.getAvailablility("home");
	}

	@ShellMethod(key = { "create" }, value = "Shorthand for '<context> create'. Available in 'order' and 'idpa app'.")
	@ShellMethodAvailability({ "createAvailability" })
	public AttributedString create(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String resourceOrType, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId)
			throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("create");
		return shorthand.execute(resourceOrType, appId);
	}

	public Availability createAvailability() {
		return contextManager.getAvailablility("create");
	}

	@ShellMethod(key = { "update" }, value = "Shorthand for '<context> update'. Available in 'idpa app'.")
	@ShellMethodAvailability({ "updateAvailability" })
	public AttributedString update(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String resource, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId,
			@ShellOption(defaultValue = "false", value = { "--add", "-a" }, help = "Consider element additions.") boolean add,
			@ShellOption(defaultValue = "false", value = { "--remove", "-r" }, help = "Consider element removals.") boolean remove,
			@ShellOption(defaultValue = "false", value = { "--change", "-c" }, help = "Consider element changes.") boolean change,
			@ShellOption(defaultValue = "false", value = { "--endpoints", "-e" }, help = "Consider endpoints.") boolean endpoints,
			@ShellOption(defaultValue = "false", value = { "--parameters", "-p" }, help = "Consider parameters.") boolean parameters,
			@ShellOption(defaultValue = "false", value = { "--hide-ignored" }, help = "Consider parameters.") boolean hideIgnored) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("update");
		return shorthand.execute(resource, appId, add, remove, change, endpoints, parameters, hideIgnored);
	}

	public Availability updateAvailability() {
		return contextManager.getAvailablility("update");
	}

	@ShellMethod(key = { "submit" }, value = "Shorthand for '<context> submit'. Available in 'order'.")
	@ShellMethodAvailability({ "submitAvailability" })
	public AttributedString submit() throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("submit");
		return shorthand.execute();
	}

	public Availability submitAvailability() {
		return contextManager.getAvailablility("submit");
	}

	@ShellMethod(key = { "wait" }, value = "Shorthand for '<context> wait'. Available in 'order'.")
	@ShellMethodAvailability({ "waitAvailability" })
	public AttributedString wait(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String timeout, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("wait");
		return shorthand.execute(timeout, id);
	}

	public Availability waitAvailability() {
		return contextManager.getAvailablility("wait");
	}

	@ShellMethod(key = { "report" }, value = "Shorthand for '<context> report'. Available in 'order'.")
	@ShellMethodAvailability({ "reportAvailability" })
	public AttributedString report(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("report");
		return shorthand.execute(id);
	}

	public Availability reportAvailability() {
		return contextManager.getAvailablility("report");
	}

	@ShellMethod(key = { "clean" }, value = "Shorthand for '<context> clean'. Available in 'order'.")
	@ShellMethodAvailability({ "cleanAvailability" })
	public AttributedString clear(@ShellOption(value = { "--current", "-c" }, defaultValue = "false") boolean cleanCurrent) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("clean");
		return shorthand.execute(cleanCurrent);
	}

	public Availability cleanAvailability() {
		return contextManager.getAvailablility("clean");
	}

	@ShellMethod(key = { "check" }, value = "Shorthand for '<context> check'. Available in 'idpa ann'.")
	@ShellMethodAvailability({ "checkAvailability" })
	public AttributedString check(@ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("check");
		return shorthand.execute(appId);
	}

	public Availability checkAvailability() {
		return contextManager.getAvailablility("check");
	}

	@ShellMethod(key = { "unify" }, value = "Shorthand for '<context> unify'. Available in 'accesslogs'.")
	@ShellMethodAvailability({ "unifyAvailability" })
	public AttributedString unify(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String path, @ShellOption(value = "app-id", defaultValue = Shorthand.DEFAULT_VALUE) String appId)
			throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("unify");
		return shorthand.execute(path, appId);
	}

	public Availability unifyAvailability() {
		return contextManager.getAvailablility("unify");
	}

}
