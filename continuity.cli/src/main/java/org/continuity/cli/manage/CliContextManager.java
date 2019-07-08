package org.continuity.cli.manage;

import java.text.ParseException;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.Availability;
import org.springframework.shell.jline.PromptProvider;

/**
 *
 * @author Henning Schulz
 *
 */
public class CliContextManager implements PromptProvider {

	public static final String ROOT_PROMPT = "continuity";

	private static final String FALLBACK_NAME = "fallback";

	private static final String DEFAULT_VERSION = "latest";

	private final Stack<CliContext> context = new Stack<>();

	private AppId currentAppId;

	private String currentVersion;

	public void setCurrentAppId(AppId currentAppId) {
		this.currentAppId = currentAppId;
	}

	public AppId getCurrentAppId() {
		return currentAppId;
	}

	public void setCurrentVersion(String currentVersion) {
		if (currentVersion != null) {
			try {
				VersionOrTimestamp.fromString(currentVersion);
			} catch (ParseException | NumberFormatException e) {
				throw new IllegalArgumentException("Illegally formatted version or timestamp '" + currentVersion + "'!");
			}
		}

		this.currentVersion = currentVersion;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public String getCurrentVersionOrLatest() {
		return currentVersion == null ? DEFAULT_VERSION : currentVersion;
	}

	public String getVersionOrFail(String passedVersion) {
		if (Shorthand.DEFAULT_VALUE.equals(passedVersion)) {
			if (currentAppId == null) {
				throw new IllegalArgumentException("Cannot use the default version. There is no version set! Please call 'version <your app-id>' first or define it as a parameter of the command.");
			} else {
				return currentVersion;
			}
		} else {
			return passedVersion;
		}
	}

	/**
	 * Gets an app-id out of the input or throws an {@link IllegalArgumentException} if no app-id
	 * can be determined.
	 *
	 * @param passedAid
	 *            An app-id received from a command.
	 * @return The app-id if present and not equal to {@link Shorthand#DEFAULT_VALUE} or the current
	 *         app-id otherwise.
	 */
	public AppId getAppIdOrFail(String passedAid) {
		if (Shorthand.DEFAULT_VALUE.equals(passedAid)) {
			if (currentAppId == null) {
				throw new IllegalArgumentException("Cannot use the default app-id. There is no app-id set! Please call 'app-id <your app-id>' first or define it as a parameter of the command.");
			} else {
				return currentAppId;
			}
		} else {
			return AppId.fromString(passedAid);
		}
	}

	/**
	 * Adds a new context.
	 *
	 * @param ctxt
	 */
	public void addContext(CliContext ctxt) {
		context.push(ctxt);
	}

	/**
	 * Goes to a context discarding the current hierarchy.
	 *
	 * @param ctxt
	 */
	public void goToContext(CliContext... ctxt) {
		context.clear();

		for (CliContext c : ctxt) {
			addContext(c);
		}
	}

	/**
	 * Removes the context that is on top of the stack.
	 *
	 * @return Whether there was a context to be removed.
	 */
	public boolean removeTopmostContext() {
		try {
			context.pop();
			return true;
		} catch (EmptyStackException e) {
			return false;
		}
	}

	/**
	 * Gets the shorthand of the current context for a given shorthand name and type.
	 *
	 * @param shorthandName
	 * @param type
	 * @return
	 */
	public Shorthand getShorthand(String shorthandName) {
		if (context.isEmpty() || !context.peek().getShorthands().containsKey(shorthandName)) {
			return fallbackShorthand(shorthandName);
		} else {
			return context.peek().getShorthands().get(shorthandName);
		}
	}

	public Map<String, Shorthand> getAllAvailableShorthands() {
		if (context.isEmpty()) {
			return Collections.emptyMap();
		} else {
			return context.peek().getShorthands();
		}
	}

	public Availability getAvailablility(String shorthandName) {
		Shorthand shorthand = getShorthand(shorthandName);

		return FALLBACK_NAME.equals(shorthand.getCommandName()) ? Availability.unavailable("there is no such shorthand in " + currentPrompt() + ".") : Availability.available();
	}

	private Shorthand fallbackShorthand(String shorthandName) {
		return new Shorthand(shorthandName, FALLBACK_NAME);
	}

	private StringBuilder currentPrompt() {
		StringBuilder builder = new StringBuilder();
		builder.append(ROOT_PROMPT);

		String contextString = context.stream().map(CliContext::getName).collect(Collectors.joining("/"));

		if (!contextString.isEmpty()) {
			builder.append("/");
			builder.append(contextString);
		}

		if (currentAppId != null) {
			builder.append(" (");
			builder.append(currentAppId);
			builder.append("@").append(getCurrentVersionOrLatest());
			builder.append(")");
		} else if (currentVersion != null) {
			builder.append(" (???").append("@").append(currentVersion).append(")");
		}

		return builder;
	}

	@Override
	public AttributedString getPrompt() {
		return new AttributedString(currentPrompt().append(":> "), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
	}

}
