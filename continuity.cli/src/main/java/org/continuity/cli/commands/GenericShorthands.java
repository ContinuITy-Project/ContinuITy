package org.continuity.cli.commands;

import org.continuity.cli.manage.CliContextManager;
import org.continuity.cli.manage.Shorthand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.Availability;
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
public class GenericShorthands {

	@Autowired
	private CliContextManager contextManager;

	@ShellMethod(key = { "download" }, value = "Shorthand for '<context> download'. Available in 'idpa' and 'jmeter'.")
	@ShellMethodAvailability({ "downloadAvailability" })
	public String download(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String link) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("download");
		String defaultOrActual = shorthand.checkRequiredParameter(link, 0, "--link");
		return shorthand.execute(defaultOrActual);
	}

	public Availability downloadAvailability() {
		return contextManager.getAvailablility("download");
	}

	@ShellMethod(key = { "upload" }, value = "Shorthand for '<context> upload'. Available in 'idpa'.")
	@ShellMethodAvailability({ "uploadAvailability" })
	public String upload(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("upload");
		String defaultOrActual = shorthand.checkRequiredParameter(id, 0, "--id");
		return shorthand.execute(defaultOrActual);
	}

	public Availability uploadAvailability() {
		return contextManager.getAvailablility("upload");
	}

	@ShellMethod(key = { "init" }, value = "Shorthand for '<context> init'. Available in 'idpa'.")
	@ShellMethodAvailability({ "initAvailability" })
	public String init(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("init");
		String defaultOrActual = shorthand.checkRequiredParameter(id, 0, "--id");
		return shorthand.execute(defaultOrActual);
	}

	public Availability initAvailability() {
		return contextManager.getAvailablility("init");
	}

	@ShellMethod(key = { "edit" }, value = "Shorthand for '<context> edit'. Available in 'idpa' and 'order'.")
	@ShellMethodAvailability({ "editAvailability" })
	public String edit(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("edit");
		String defaultOrActual = shorthand.checkRequiredParameter(id, 0, "--id");
		return shorthand.execute(defaultOrActual);
	}

	public Availability editAvailability() {
		return contextManager.getAvailablility("edit");
	}

	@ShellMethod(key = { "home" }, value = "Shorthand for '<context> home'. Available in 'jmeter'.")
	@ShellMethodAvailability({ "homeAvailability" })
	public String home(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String path) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("home");
		String defaultOrActual = shorthand.checkRequiredParameter(path, 0, "--path");
		return shorthand.execute(defaultOrActual);
	}

	public Availability homeAvailability() {
		return contextManager.getAvailablility("home");
	}

	@ShellMethod(key = { "config" }, value = "Shorthand for '<context> config'. Available in 'jmeter'.")
	@ShellMethodAvailability({ "configAvailability" })
	public String config(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String path) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("config");
		String defaultOrActual = shorthand.checkRequiredParameter(path, 0, "--path");
		return shorthand.execute(defaultOrActual);
	}

	public Availability configAvailability() {
		return contextManager.getAvailablility("config");
	}

	@ShellMethod(key = { "create" }, value = "Shorthand for '<context> create'. Available in 'order'.")
	@ShellMethodAvailability({ "createAvailability" })
	public String create(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String type) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("create");
		String defaultOrActual = shorthand.checkRequiredParameter(type, 0, "--type");
		return shorthand.execute(defaultOrActual);
	}

	public Availability createAvailability() {
		return contextManager.getAvailablility("create");
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
		String defaultOrActualTimeout = shorthand.checkRequiredParameter(timeout, 0, "--timeout");
		String defaultOrActualId = shorthand.checkRequiredParameter(id, 0, "--id");
		return shorthand.execute(defaultOrActualTimeout, defaultOrActualId);
	}

	public Availability waitAvailability() {
		return contextManager.getAvailablility("wait");
	}

	@ShellMethod(key = { "report" }, value = "Shorthand for '<context> report'. Available in 'order'.")
	@ShellMethodAvailability({ "reportAvailability" })
	public String report(@ShellOption(defaultValue = Shorthand.DEFAULT_VALUE) String id) throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("report");
		String defaultOrActual = shorthand.checkRequiredParameter(id, 0, "--id");
		return shorthand.execute(defaultOrActual);
	}

	public Availability reportAvailability() {
		return contextManager.getAvailablility("report");
	}

	@ShellMethod(key = { "clean" }, value = "Shorthand for '<context> clean'. Available in 'order'.")
	@ShellMethodAvailability({ "cleanAvailability" })
	public String clean() throws Throwable {
		Shorthand shorthand = contextManager.getShorthand("clean");
		return shorthand.execute();
	}

	public Availability cleanAvailability() {
		return contextManager.getAvailablility("clean");
	}

}
