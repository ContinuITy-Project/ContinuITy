package org.continuity.commons.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A generic storage holding entities in a hash map in memory.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of the stored entities.
 */
public class MemoryStorage<T> implements ArtifactStorage<T> {

	private static final String TAG_DELIM = "-";

	private final AtomicInteger counter = new AtomicInteger(1);

	private final Map<String, T> storedEntities = new HashMap<>();

	public MemoryStorage(Class<T> type) {
	}

	@Override
	public String reserve(String tag) {
		return tag + TAG_DELIM + Integer.toString(counter.getAndIncrement());
	}

	@Override
	public void putToReserved(String id, T entity) {
		storedEntities.put(id, entity);
	}

	@Override
	public String put(T entity, String tag) {
		String id = reserve(tag);
		putToReserved(id, entity);
		return id;
	}

	@Override
	public T get(String id) {
		return storedEntities.get(id);
	}

	@Override
	public boolean remove(String id) {
		return storedEntities.remove(id) != null;
	}

	@Override
	public String getTagForId(String id) {
		return id.substring(0, id.lastIndexOf(TAG_DELIM));
	}

}
