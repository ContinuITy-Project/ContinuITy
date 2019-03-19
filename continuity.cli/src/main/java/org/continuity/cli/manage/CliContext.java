package org.continuity.cli.manage;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Henning Schulz
 *
 */
public class CliContext {

	private final String name;

	private final Map<String, Shorthand> shorthands;

	public CliContext(String name, Shorthand... shorthands) {
		this.name = name;
		this.shorthands = new HashMap<>();

		for (Shorthand sh : shorthands) {
			this.shorthands.put(sh.getShorthandName(), sh);
		}
	}

	public String getName() {
		return name;
	}

	/**
	 * Shorthands per shorthand name.
	 *
	 * @return
	 */
	public Map<String, Shorthand> getShorthands() {
		return shorthands;
	}

}
