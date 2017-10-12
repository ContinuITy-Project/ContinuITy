package org.continuity.utils.enums;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Utility class that holds a mapping of classes to enum constants. Built for usage within an enum.
 *
 * @author Henning Schulz
 *
 */
public class EnumForClassHolder<E extends Enum<E>, S> {

	private final Map<Class<? extends S>, E> enumForClass = new HashMap<>();

	private final E fallback;

	/**
	 * Creates a new instance with a fallback constant. This constant will not be put into the
	 * mapping, but used if there is no mapping.
	 *
	 * @param enumClass
	 *            The type of the enum.
	 * @param classGetter
	 *            A function that returns a collection of classes per enum.
	 * @param fallback
	 *            The fallback constant.
	 */
	public EnumForClassHolder(Class<E> enumClass, Function<E, Collection<Class<? extends S>>> classGetter, E fallback) {
		this.fallback = fallback;

		for (E constant : enumClass.getEnumConstants()) {
			if (constant == fallback) {
				continue;
			}

			for (Class<? extends S> clazz : classGetter.apply(constant)) {
				enumForClass.put(clazz, constant);
			}
		}
	}

	/**
	 * Creates a new instance without a fallback constant. As fallback, {@code null} will be used.
	 *
	 * @param enumClass
	 *            The type of the enum.
	 * @param classGetter
	 *            A function that returns a collection of classes per enum.
	 */
	public EnumForClassHolder(Class<E> enumClass, Function<E, Collection<Class<? extends S>>> classGetter) {
		this(enumClass, classGetter, null);
	}

	/**
	 * Gets an enum constants for the specified type. If there is no constant, the fallback value
	 * (of {@code null}) is returned.
	 *
	 * @param type
	 *            The type for which the enum constant is to be returned.
	 * @return The enum constant registered for the specified type.
	 */
	public E get(Class<? extends S> type) {
		E constant = enumForClass.get(type);

		if (constant == null) {
			for (Entry<Class<? extends S>, E> entry : enumForClass.entrySet()) {
				if (entry.getKey().isAssignableFrom(type)) {
					return entry.getValue();
				}
			}

			return fallback;
		} else {
			return constant;
		}
	}

}
