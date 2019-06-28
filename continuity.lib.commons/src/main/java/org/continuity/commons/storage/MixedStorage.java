package org.continuity.commons.storage;

import java.nio.file.Path;

import org.continuity.idpa.AppId;

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
	 * @param aid
	 *            The app-id of the entity.
	 * @param persist
	 *            Whether the {@link FileStorage} should be used.
	 * @return An id for the slot.
	 */
	public String reserve(AppId aid, boolean persist) {
		if (persist) {
			return toFileId(fileStorage.reserve(aid));
		} else {
			return memoryStorage.reserve(aid);
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
	 * @param aid
	 *            The app-id of the entity.
	 * @param persist
	 *            Whether the {@link FileStorage} should be used.
	 * @return The created id.
	 */
	public String put(T entity, AppId aid, boolean persist) {
		if (persist) {
			return toFileId(fileStorage.put(entity, aid));
		} else {
			return memoryStorage.put(entity, aid);
		}
	}

	@Override
	public String reserve(AppId aid) {
		return reserve(aid, false);
	}

	@Override
	public String put(T entity, AppId aid) {
		return put(entity, aid, false);
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
	public AppId getAppIdForId(String id) {
		if (isFileId(id)) {
			return fileStorage.getAppIdForId(fromFileId(id));
		} else {
			return memoryStorage.getAppIdForId(id);
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
