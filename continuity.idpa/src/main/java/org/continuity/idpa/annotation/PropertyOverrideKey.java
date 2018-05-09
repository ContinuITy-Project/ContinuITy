package org.continuity.idpa.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.continuity.idpa.IdpaElement;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

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
	 * Converts a key to a printable string, e.g. {@code HttpEndpoint.domain}.
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
	@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY)
	@JsonSubTypes({ @Type(value = HttpEndpoint.class), @Type(value = HttpParameter.class) })
	public static interface Any {

		/**
		 * Gets the value for the represented key from an instance of T.
		 *
		 * @param instance
		 *            The instance object.
		 * @return The value of the instance for the key.
		 */
		default String getFromInstance(IdpaElement instance) {
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
		default void setToInstance(IdpaElement instance, String value) {
			try {
				Method setter = instance.getClass().getMethod("set" + formatName(name(), "", true), String.class);
				setter.invoke(instance, value);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Returns whether this key constant is relevant for the passed scope. E.g., <br>
		 * {@link HttpEndpoint#DOMAIN}{@code .isInScope(}{@link Any}{@code .class) == true}, but
		 * <br>
		 * {@link HttpEndpoint#DOMAIN}{@code .isInScope(}{@link HttpParameter}{@code .class) == false}.
		 *
		 * @param scope
		 *            The scope to test.
		 * @return {@code true}, if the constant is of a subtype of the scope or {@code false},
		 *         otherwise.
		 */
		default boolean isInScope(Class<? extends Any> scope) {
			return scope.isAssignableFrom(getClass());
		}

		String name();
	}

	public static interface EndpointLevel extends Any {
	}

	public static interface ParameterLevel extends EndpointLevel {
	}

	/**
	 * Keys of {@link org.continuity.idpa.application.HttpEndpoint HttpEndpoint}s.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static enum HttpEndpoint implements EndpointLevel {

		DOMAIN, PORT, ENCODING, PROTOCOL, HEADER;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return toPrintableString(this);
		}

	}

	/**
	 * Keys of {@link org.continuity.idpa.application.HttpParameter HttpParameter}s.
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
