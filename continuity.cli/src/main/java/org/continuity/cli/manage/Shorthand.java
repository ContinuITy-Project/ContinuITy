package org.continuity.cli.manage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jline.utils.AttributedString;
import org.springframework.core.MethodParameter;
import org.springframework.shell.ParameterDescription;
import org.springframework.shell.ParameterMissingResolutionException;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 *
 * @author Henning Schulz
 *
 */
public class Shorthand implements Comparable<Shorthand> {

	public static final String DEFAULT_VALUE = "_DEFAULT_";

	private final String shorthandName;

	private final String commandName;

	private final Object controller;

	private final Method method;

	private final int numParameters;

	/**
	 * Constructor.
	 *
	 * @param shorthandName
	 *            The name of the shorthand.
	 * @param controller
	 *            The shell controller implementing the commands.
	 * @param methodName
	 *            The method name of the command implementation. Needs to be annotated with
	 *            {@link ShellMethod}.
	 * @param parameterTypes
	 *            The parameter types of the method.
	 */
	public Shorthand(String shorthandName, Object controller, String methodName, Class<?>... parameterTypes) {
		this.shorthandName = shorthandName;
		this.controller = controller;
		try {
			this.method = controller.getClass().getMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("Could not initialize shorthand " + shorthandName + " for controller of type " + controller.getClass(), e);
		}

		this.commandName = this.method.getAnnotation(ShellMethod.class).key()[0];
		this.numParameters = parameterTypes.length;
	}

	public Shorthand(String shorthandName, String commandName) {
		this.shorthandName = shorthandName;
		this.commandName = commandName;
		this.controller = null;
		this.method = null;
		this.numParameters = 0;
	}

	public String getShorthandName() {
		return shorthandName;
	}

	public String getCommandName() {
		return commandName;
	}

	public Method getMethod() {
		return method;
	}

	/**
	 * Executes the command.
	 *
	 * @param args
	 *            The arguments.
	 * @return
	 */
	public AttributedString execute(Object... args) throws Throwable {
		List<Object> params = new ArrayList<>();
		int i = 0;

		for (Object arg : args) {
			if (i < numParameters) {
				params.add(checkRequiredParameter(arg, i));
			}

			i++;
		}

		Object returnValue;

		try {
			returnValue = method.invoke(controller, params.toArray(new Object[numParameters]));
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}

		if (returnValue instanceof AttributedString) {
			return (AttributedString) returnValue;
		} else {
			return new AttributedString(Objects.toString(returnValue));
		}
	}

	/**
	 * Returns the default value if present or {@code null} otherwise.
	 *
	 * @param index
	 *            Index of the parameter.
	 * @return
	 */
	public String getDefaultValue(int index) {
		ShellOption shellOption = method.getParameters()[index].getAnnotation(ShellOption.class);
		return shellOption == null ? null : shellOption.defaultValue();
	}

	private String getParameterName(int index) {
		return method.getParameters()[index].getName();
	}

	/**
	 * Checks whether the specified parameter is required. If so and if it is the
	 * {@link #DEFAULT_VALUE}, a {@link ParameterMissingResolutionException} is thrown. Otherwise,
	 * the value itself or the default value of the target method is returned.
	 *
	 * @param value
	 *            The value received by the shorthand.
	 * @param paramIndex
	 *            The parameter index.
	 * @return {@code value} or the default value of the target method (if {@code value} is
	 *         {@code null}).
	 */
	private Object checkRequiredParameter(Object value, int paramIndex) {
		if (DEFAULT_VALUE.equals(value)) {
			String defaultValue = getDefaultValue(paramIndex);

			if (defaultValue == null) {
				try {
					throw new ParameterMissingResolutionException(
							ParameterDescription.outOf(MethodParameter.forExecutable(getMethod(), paramIndex)).keys(Collections.singletonList(getParameterName(paramIndex))));
				} catch (SecurityException e) {
					e.printStackTrace();
				}
			}

			switch (value.getClass().getSimpleName()) {
			case "Boolean":
				return Boolean.parseBoolean(defaultValue);
			case "Integer":
				return Integer.parseInt(defaultValue);
			case "Long":
				return Long.parseLong(defaultValue);
			case "Double":
				return Double.parseDouble(defaultValue);
			default:
				return defaultValue;
			}
		} else {
			return value;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Shorthand) {
			Shorthand other = (Shorthand) obj;
			return Objects.equals(this.getShorthandName(), other.getShorthandName());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(getShorthandName());
	}

	@Override
	public int compareTo(Shorthand other) {
		return this.getShorthandName().compareTo(other.getShorthandName());
	}

}
