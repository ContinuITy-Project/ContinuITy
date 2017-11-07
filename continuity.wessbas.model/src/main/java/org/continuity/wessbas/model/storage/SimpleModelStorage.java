package org.continuity.wessbas.model.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import m4jdsl.WorkloadModel;

/**
 * Simple in-memory storage for {@link WorkloadModel}s.
 *
 * @author Henning Schulz
 *
 */
public class SimpleModelStorage {

	private static SimpleModelStorage instance;

	private final AtomicInteger counter = new AtomicInteger(1);

	private final Map<String, WorkloadModel> storedModels = new HashMap<>();

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
	 * Adds a new workload model and returns the automatically created id.
	 *
	 * @param model
	 *            The model to be stored.
	 * @return The created id.
	 */
	public String put(WorkloadModel model) {
		String id = Integer.toString(counter.getAndIncrement());
		storedModels.put(id, model);
		return id;
	}

	/**
	 * Retrieves the model for the given id.
	 *
	 * @param id
	 *            The id of the model.
	 * @return A {@link WorkloadModel}.
	 */
	public WorkloadModel get(String id) {
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
