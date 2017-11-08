package org.continuity.utils.enums;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

/**
 * Utility class that holds a mapping of classes to enum constants. Built for usage within an enum.
 *
 * @author Henning Schulz
 *
 */
public class EnumForClassHolder<E extends Enum<E>, S> {

	private final Map<Class<? extends S>, Set<E>> enumForClass = new HashMap<>();

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
				addConstant(clazz, constant);
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
	 * Gets the enum constants for the specified type. If there is no constant, the fallback value
	 * (or {@code null}) is returned.
	 *
	 * @param type
	 *            The type for which the enum constants are to be returned.
	 * @return The enum constants registered for the specified type.
	 */
	public Set<E> get(Class<? extends S> type) {
		Set<E> result = new HashSet<>();

		for (Entry<Class<? extends S>, Set<E>> entry : enumForClass.entrySet()) {
			if (entry.getKey().isAssignableFrom(type)) {
				result.addAll(entry.getValue());
			}
		}

		if (!result.isEmpty()) {
			return result;
		} else {
			return Collections.singleton(fallback);
		}
	}

	/**
	 * Gets a single random enum constant for the specified type. If there is no constant, the
	 * fallback value (or {@code null}) is returned.
	 *
	 * @param type
	 *            The type for which the enum constant is to be returned.
	 * @return One enum constant registered for the specified type.
	 */
	public E getOne(Class<? extends S> type) {
		for (E constant : get(type)) {
			return constant;
		}

		return fallback;
	}

	private void addConstant(Class<? extends S> clazz, E constant) {
		Set<E> list = enumForClass.get(clazz);

		if (list == null) {
			list = new HashSet<>();
			enumForClass.put(clazz, list);
		}

		list.add(constant);
	}

}
