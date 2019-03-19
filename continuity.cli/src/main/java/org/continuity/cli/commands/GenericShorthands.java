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
	public String download(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String link) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("download");
		return shorthand.execute(link);
	}

	public Availability downloadAvailability() {
		return contextManager.getAvailablility("download");
	}

	@ShellMethod(key = { "upload" }, value = "Shorthand for '<context> upload'. Available in 'idpa'.")
	@ShellMethodAvailability({ "uploadAvailability" })
	public String upload(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("upload");
		return shorthand.execute(id);
	}

	public Availability uploadAvailability() {
		return contextManager.getAvailablility("upload");
	}

	@ShellMethod(key = { "set" }, value = "Shorthand for '<context> set'. Available in 'tag'.")
	@ShellMethodAvailability({ "setAvailability" })
	public void set(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String value) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("set");
		shorthand.execute(value);
	}

	public Availability setAvailability() {
		return contextManager.getAvailablility("set");
	}

	@ShellMethod(key = { "init" }, value = "Shorthand for '<context> init'. Available in 'idpa ann'.")
	@ShellMethodAvailability({ "initAvailability" })
	public String init(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("init");
		return shorthand.execute(tag);
	}

	public Availability initAvailability() {
		return contextManager.getAvailablility("init");
	}

	@ShellMethod(key = { "open" }, value = "Shorthand for '<context> open'. Available in 'idpa' and 'order'.")
	@ShellMethodAvailability({ "openAvailability" })
	public String open(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("open");
		return shorthand.execute(id);
	}

	public Availability openAvailability() {
		return contextManager.getAvailablility("open");
	}

	@ShellMethod(key = { "home" }, value = "Shorthand for '<context> home'. Available in 'jmeter'.")
	@ShellMethodAvailability({ "homeAvailability" })
	public String home(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String path) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("home");
		return shorthand.execute(path);
	}

	public Availability homeAvailability() {
		return contextManager.getAvailablility("home");
	}

	@ShellMethod(key = { "config" }, value = "Shorthand for '<context> config'. Available in 'jmeter'.")
	@ShellMethodAvailability({ "configAvailability" })
	public String config(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String path) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("config");
		return shorthand.execute(path);
	}

	public Availability configAvailability() {
		return contextManager.getAvailablility("config");
	}

	@ShellMethod(key = { "create" }, value = "Shorthand for '<context> create'. Available in 'order' and 'idpa app'.")
	@ShellMethodAvailability({ "createAvailability" })
	public String create(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String resourceOrType, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("create");
		return shorthand.execute(resourceOrType, tag);
	}

	public Availability createAvailability() {
		return contextManager.getAvailablility("create");
	}

	@ShellMethod(key = { "update" }, value = "Shorthand for '<context> update'. Available in 'idpa app'.")
	@ShellMethodAvailability({ "updateAvailability" })
	public String update(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String resource, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag,
			@ShellOption(defaultValue = "false", value = { "--add", "-a" }, help = "Consider element additions.") boolean add,
			@ShellOption(defaultValue = "false", value = { "--remove", "-r" }, help = "Consider element removals.") boolean remove,
			@ShellOption(defaultValue = "false", value = { "--change", "-c" }, help = "Consider element changes.") boolean change,
			@ShellOption(defaultValue = "false", value = { "--endpoints", "-e" }, help = "Consider endpoints.") boolean endpoints,
			@ShellOption(defaultValue = "false", value = { "--parameters", "-p" }, help = "Consider parameters.") boolean parameters,
			@ShellOption(defaultValue = "false", value = { "--hide-ignored" }, help = "Consider parameters.") boolean hideIgnored) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("update");
		return shorthand.execute(resource, tag, add, remove, change, endpoints, parameters, hideIgnored);
	}

	public Availability updateAvailability() {
		return contextManager.getAvailablility("update");
	}

	@ShellMethod(key = { "submit" }, value = "Shorthand for '<context> submit'. Available in 'order'.")
	@ShellMethodAvailability({ "submitAvailability" })
	public String submit() throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("submit");
		return shorthand.execute();
	}

	public Availability submitAvailability() {
		return contextManager.getAvailablility("submit");
	}

	@ShellMethod(key = { "wait" }, value = "Shorthand for '<context> wait'. Available in 'order'.")
	@ShellMethodAvailability({ "waitAvailability" })
	public String wait(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String timeout, @ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("wait");
		return shorthand.execute(timeout, id);
	}

	public Availability waitAvailability() {
		return contextManager.getAvailablility("wait");
	}

	@ShellMethod(key = { "report" }, value = "Shorthand for '<context> report'. Available in 'order'.")
	@ShellMethodAvailability({ "reportAvailability" })
	public String report(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("report");
		return shorthand.execute(id);
	}

	public Availability reportAvailability() {
		return contextManager.getAvailablility("report");
	}

	@ShellMethod(key = { "clean" }, value = "Shorthand for '<context> clean'. Available in 'order'.")
	@ShellMethodAvailability({ "cleanAvailability" })
	public String clear() throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("clean");
		return shorthand.execute();
	}

	public Availability cleanAvailability() {
		return contextManager.getAvailablility("clean");
	}

	@ShellMethod(key = { "reset" }, value = "Shorthand for '<context> clean'. Available in 'tag'.")
	@ShellMethodAvailability({ "resetAvailability" })
	public void reset() throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("reset");
		shorthand.execute();
	}

	public Availability resetAvailability() {
		return contextManager.getAvailablility("reset");
	}

	@ShellMethod(key = { "check" }, value = "Shorthand for '<context> check'. Available in 'idpa ann'.")
	@ShellMethodAvailability({ "checkAvailability" })
	public String check(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String tag) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("check");
		return shorthand.execute(tag);
	}

	public Availability checkAvailability() {
		return contextManager.getAvailablility("check");
	}

}
