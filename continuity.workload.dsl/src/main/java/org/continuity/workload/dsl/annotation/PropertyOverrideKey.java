package org.continuity.workload.dsl.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.continuity.workload.dsl.ContinuityModelElement;

/**
 * Holds key enums for {@link PropertyOverride}s.
 *
 * @author Henning Schulz
 *
 */
public class PropertyOverrideKey {

	private PropertyOverrideKey() {
		// should not be instantiated
	}

	/**
	 * Converts a key to a printable string, e.g. {@code HttpInterface.domain}.
	 *
	 * @param key
	 *            The key to be printed.
	 * @return A formatted string.
	 */
	public static String toPrintableString(Any key) {
		return key.getClass().getSimpleName() + "." + formatName(key.name(), "-", false);
	}

	/**
	 * Reads a key from a format as it is written by {@link #toPrintableString(Any)}.
	 *
	 * @param string
	 *            The string to be parsed.
	 * @return The parsed key.
	 */
	public static Any fromPrintableString(String string) {
		String[] typeAndConstant = string.split("\\.");
		String className = PropertyOverrideKey.class.getName() + "$" + typeAndConstant[0];
		Class<?> clazz = null;

		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Class " + typeAndConstant[0] + " from input " + string + " does not exist!");
		}

		if (!Any.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Class " + clazz.getName() + " is not a subtype of PeropertyOverrideKey.Any!");
		}

		for (Object constant : clazz.getEnumConstants()) {
			if (constant.toString().equals(string)) {
				return (Any) constant;
			}
		}

		return null;
	}

	/**
	 * Common superclass of all key enums.
	 *
	 * @author Henning Schulz
	 *
	 * @param <T>
	 *            The type of element holding the overridden value.
	 */
	public static interface Any {

		/**
		 * Gets the value for the represented key from an instance of T.
		 *
		 * @param instance
		 *            The instance object.
		 * @return The value of the instance for the key.
		 */
		default String getFromInstance(ContinuityModelElement instance) {
			Object result = null;

			try {
				Method getter = instance.getClass().getMethod("get" + formatName(name(), "", true));
				result = getter.invoke(instance);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}

			if (result == null) {
				return null;
			} else {
				return result.toString();
			}
		}

		/**
		 * Sets the value for the represented key to an instance of T.
		 *
		 * @param instance
		 *            The instance object.
		 * @param value
		 *            The value to be set.
		 */
		default void setToInstance(ContinuityModelElement instance, String value) {
			try {
				Method setter = instance.getClass().getMethod("set" + formatName(name(), "", true), String.class);
				setter.invoke(instance, value);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		String name();
	}

	public static interface InterfaceLevel extends Any {
	}

	public static interface ParameterLevel extends InterfaceLevel {
	}

	/**
	 * Keys of {@link org.continuity.workload.dsl.system.HttpInterface HttpInterface}s.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static enum HttpInterface implements InterfaceLevel {

		DOMAIN, PORT, ENCODING, PROTOCOL;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return toPrintableString(this);
		}

	}

	/**
	 * Keys of {@link org.continuity.workload.dsl.system.HttpParameter HttpParameter}s.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static enum HttpParameter implements ParameterLevel {

		TYPE, ENCODED;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return toPrintableString(this);
		}

	}

	private static String formatName(String name, String separator, boolean upperCases) {
		StringBuilder builder = new StringBuilder();

		String[] tokens = name.split("_");

		boolean first = true;

		for (String tok : tokens) {
			if (first) {
				first = false;
			} else {
				builder.append(separator);
			}

			if (upperCases) {
				builder.append(tok.substring(0, 1).toUpperCase());
				builder.append(tok.substring(1).toLowerCase());
			} else {
				builder.append(tok.toLowerCase());
			}
		}

		return builder.toString();
	}

}
