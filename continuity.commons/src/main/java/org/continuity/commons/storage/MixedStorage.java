package org.continuity.commons.storage;

import java.nio.file.Path;

public class MixedStorage<T> implements ArtifactStorage<T> {

	private static final String FILE_PREFIX = "_persisted_";

	private final MemoryStorage<T> memoryStorage;

	private final FileStorage<T> fileStorage;

	public MixedStorage(Class<T> entityType, FileStorage<T> fileStorage) {
		this.memoryStorage = new MemoryStorage<>(entityType);
		this.fileStorage = fileStorage;
	}

	/**
	 * Uses the default {@link JsonFileStorage}.
	 *
	 * @param storagePath
	 * @param emptyEntity
	 * @param entityType
	 */
	public MixedStorage(Path storagePath, T emptyEntity, Class<T> entityType) {
		this(entityType, new JsonFileStorage<T>(storagePath, emptyEntity, entityType));
	}

	/**
	 * Uses the default {@link JsonFileStorage}.
	 *
	 * @param storagePath
	 * @param emptyEntity
	 */
	@SuppressWarnings("unchecked")
	public MixedStorage(Path storagePath, T emptyEntity) {
		this(storagePath, emptyEntity, (Class<T>) emptyEntity.getClass());
	}

	/**
	 * Reserves a slot in the storage.
	 *
	 * @param tag
	 *            The tag of the entity.
	 * @param persist
	 *            Whether the {@link FileStorage} should be used.
	 * @return An id for the slot.
	 */
	public String reserve(String tag, boolean persist) {
		if (persist) {
			return toFileId(fileStorage.reserve(tag));
		} else {
			return memoryStorage.reserve(tag);
		}
	}

	@Override
	public void putToReserved(String id, T entity) {
		if (isFileId(id)) {
			fileStorage.putToReserved(fromFileId(id), entity);
		} else {
			memoryStorage.putToReserved(id, entity);
		}
	}

	/**
	 * Adds a new entity and returns the automatically created id.
	 *
	 * @param entity
	 *            The entity to be stored.
	 * @param tag
	 *            The tag of the entity.
	 * @param persist
	 *            Whether the {@link FileStorage} should be used.
	 * @return The created id.
	 */
	public String put(T entity, String tag, boolean persist) {
		if (persist) {
			return toFileId(fileStorage.put(entity, tag));
		} else {
			return memoryStorage.put(entity, tag);
		}
	}

	@Override
	public String reserve(String tag) {
		return reserve(tag, false);
	}

	@Override
	public String put(T entity, String tag) {
		return put(entity, tag, false);
	}

	@Override
	public T get(String id) {
		if (isFileId(id)) {
			return fileStorage.get(fromFileId(id));
		} else {
			return memoryStorage.get(id);
		}
	}

	@Override
	public boolean remove(String id) {
		if (isFileId(id)) {
			return fileStorage.remove(fromFileId(id));
		} else {
			return memoryStorage.remove(id);
		}
	}

	@Override
	public String getTagForId(String id) {
		if (isFileId(id)) {
			return fileStorage.getTagForId(fromFileId(id));
		} else {
			return memoryStorage.getTagForId(id);
		}
	}

	private boolean isFileId(String id) {
		return id.startsWith(FILE_PREFIX);
	}

	private String toFileId(String id) {
		return FILE_PREFIX + id;
	}

	private String fromFileId(String fileId) {
		return fileId.substring(FILE_PREFIX.length());
	}

}
