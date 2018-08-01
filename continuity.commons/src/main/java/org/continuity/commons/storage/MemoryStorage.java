package org.continuity.commons.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import m4jdsl.WorkloadModel;

/**
 * A generic storage holding entities in a hash map in memory.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of the stored entities.
 */
public class MemoryStorage<T> {

	private static final String TAG_DELIM = "-";

	private final AtomicInteger counter = new AtomicInteger(1);

	private final Map<String, T> storedEntities = new HashMap<>();

	public MemoryStorage(Class<T> type) {
		// Should not be created from outside.
	}

	/**
	 * Reserves a slot in the storage.
	 *
	 * @param tag
	 *            The tag of the entity.
	 * @return An id for the slot.
	 */
	public String reserve(String tag) {
		return tag + TAG_DELIM + Integer.toString(counter.getAndIncrement());
	}

	/**
	 * Adds a new entity to an already reserved slot.
	 *
	 * @param id
	 *            The ID of the slot.
	 * @param entity
	 *            The entity to be stored.
	 */
	public void putToReserved(String id, T entity) {
		storedEntities.put(id, entity);
	}

	/**
	 * Adds a new entity and returns the automatically created id.
	 *
	 * @param entity
	 *            The entity to be stored.
	 * @param tag
	 *            The tag of the entity.
	 * @return The created id.
	 */
	public String put(T entity, String tag) {
		String id = reserve(tag);
		putToReserved(id, entity);
		return id;
	}

	/**
	 * Retrieves the model for the given id.
	 *
	 * @param id
	 *            The id of the model.
	 * @return A {@link WorkloadModel}.
	 */
	public T get(String id) {
		return storedEntities.get(id);
	}

	/**
	 * Removes the model with the passed id and returns whether the id was present.
	 *
	 * @param id
	 *            The id to be removed.
	 * @return {@code true}, if the passed id was present and successfully removed.
	 */
	public boolean remove(String id) {
		return storedEntities.remove(id) != null;
	}

}
