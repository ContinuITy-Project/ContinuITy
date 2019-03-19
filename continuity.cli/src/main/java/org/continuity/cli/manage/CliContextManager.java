package org.continuity.cli.manage;

import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

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

	private final Stack<CliContext> context = new Stack<>();

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

	private String currentPrompt() {
		String subPrompt = context.stream().map(CliContext::getName).collect(Collectors.joining("/"));
		return ROOT_PROMPT + (subPrompt.isEmpty() ? "" : "/" + subPrompt);
	}

	@Override
	public AttributedString getPrompt() {
		return new AttributedString(currentPrompt() + ":> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
	}

}
