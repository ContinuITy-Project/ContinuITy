package org.continuity.annotation.dsl.ann;

/**
 * States to override a specific property with a new value.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of the element whose property is to be overriden.
 *
 * @see PropertyOverrideKey
 */
public class PropertyOverride<T extends PropertyOverrideKey.Any> {

	private T key;

	private String value;

	/**
	 * Gets the key.
	 *
	 * @see PropertyOverrideKey
	 *
	 * @return The key.
	 */
	public T getKey() {
		return this.key;
	}

	/**
	 * Sets the key.
	 *
	 * @see PropertyOverrideKey
	 *
	 * @param key
	 *            New key.
	 */
	public void setKey(T key) {
		this.key = key;
	}

	/**
	 * Gets the value that overrides the original one.
	 *
	 * @return The value.
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Sets the value that overrides the original one.
	 *
	 * @param value
	 *            New value.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.valueOf(key) + ": " + value;
	}

}
