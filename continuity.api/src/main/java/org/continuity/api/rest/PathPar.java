package org.continuity.api.rest;

/**
 * Represents a path variable, e.g., <code>{id}</code> in <code>/my/path/{id}</code>.
 *
 * @author Henning Schulz
 *
 */
public class PathPar {

	private final String name;

	private PathPar(String name) {
		this.name = name;
	}

	/**
	 * Creates a new instance.
	 *
	 * @param name
	 *            The name of the parameter.
	 * @return The instance.
	 */
	public static PathPar of(String name) {
		return new PathPar(name);
	}

	/**
	 * Returns a string representation of this parameter for usage in a generic path, e.g.,
	 * <code>/my/path/{id}</code>..
	 *
	 * @return <code>{parameter-name}</code>
	 */
	public String generic() {
		return "{" + name + "}";
	}

}
