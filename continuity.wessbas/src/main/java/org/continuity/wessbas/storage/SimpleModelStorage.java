package org.continuity.wessbas.storage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.continuity.wessbas.entities.WorkloadModelStorageEntry;

import m4jdsl.WorkloadModel;

/**
 * Simple in-memory storage for {@link WorkloadModel}s.
 *
 * @author Henning Schulz
 *
 */
public class SimpleModelStorage {

	private static final String TAG_DELIM = "-";

	private static SimpleModelStorage instance;

	private final AtomicInteger counter = new AtomicInteger(1);

	private final Map<String, WorkloadModelStorageEntry> storedModels = new HashMap<>();

	private SimpleModelStorage() {
		// Should not be created from outside.
	}

	/**
	 * Gets the instance.
	 *
	 * @return The instance.
	 */
	public static SimpleModelStorage instance() {
		if (instance == null) {
			synchronized (SimpleModelStorage.class) {
				if (instance == null) {
					instance = new SimpleModelStorage();
				}
			}
		}

		return instance;
	}

	/**
	 * Reserves a slot in the storage.
	 *
	 * @param tag
	 *            The tag of the model.
	 * @return An id for the slot.
	 */
	public String reserve(String tag) {
		return tag + TAG_DELIM + Integer.toString(counter.getAndIncrement());
	}

	/**
	 * Adds a new workload model with the specified id. The id should be reserved by using
	 * {@link #reserve()}.
	 *
	 * @param id
	 *            The id of the slot.
	 * @param timestamp
	 *            The time stamp of the data used to create the model.
	 * @param model
	 *            The model to be stored.
	 */
	public void put(String id, Date timestamp, WorkloadModel model) {
		WorkloadModelStorageEntry entry = new WorkloadModelStorageEntry();
		entry.setWorkloadModel(model);
		entry.setId(id);
		entry.setCreatedDate(new Date());
		entry.setDataTimestamp(timestamp);

		storedModels.put(id, entry);
	}

	/**
	 * Adds a new workload model and returns the automatically created id.
	 *
	 * @param model
	 *            The model to be stored.
	 * @param tag
	 *            The tag of the model.
	 * @param timestamp
	 *            The time stamp of the data used to create the model.
	 * @return The created id.
	 */
	public String put(WorkloadModel model, String tag, Date timestamp) {
		String id = reserve(tag);
		put(id, timestamp, model);
		return id;
	}

	/**
	 * Retrieves the model for the given id.
	 *
	 * @param id
	 *            The id of the model.
	 * @return A {@link WorkloadModel}.
	 */
	public WorkloadModelStorageEntry get(String id) {
		return storedModels.get(id);
	}

	/**
	 * Removes the model with the passed id and returns whether the id was present.
	 *
	 * @param id
	 *            The id to be removed.
	 * @return {@code true}, if the passed id was present and successfully removed.
	 */
	public boolean remove(String id) {
		return storedModels.remove(id) != null;
	}

}
