package org.continuity.commons.storage;

import m4jdsl.WorkloadModel;

public interface ArtifactStorage<T> {

	/**
	 * Reserves a slot in the storage.
	 *
	 * @param tag
	 *            The tag of the entity.
	 * @return An id for the slot.
	 */
	String reserve(String tag);

	/**
	 * Adds a new entity to an already reserved slot.
	 *
	 * @param id
	 *            The ID of the slot.
	 * @param entity
	 *            The entity to be stored.
	 */
	void putToReserved(String id, T entity);

	/**
	 * Adds a new entity and returns the automatically created id.
	 *
	 * @param entity
	 *            The entity to be stored.
	 * @param tag
	 *            The tag of the entity.
	 * @return The created id.
	 */
	String put(T entity, String tag);

	/**
	 * Retrieves the model for the given id.
	 *
	 * @param id
	 *            The id of the model.
	 * @return A {@link WorkloadModel}.
	 */
	T get(String id);

	/**
	 * Removes the model with the passed id and returns whether the id was present.
	 *
	 * @param id
	 *            The id to be removed.
	 * @return {@code true}, if the passed id was present and successfully removed.
	 */
	boolean remove(String id);

	/**
	 * Returns the tag that is part of the ID.
	 * 
	 * @param id
	 *            The ID containing the tag.
	 * @return The tag
	 */
	String getTagForId(String id);

}
