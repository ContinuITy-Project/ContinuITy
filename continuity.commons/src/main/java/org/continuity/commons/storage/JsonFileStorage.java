package org.continuity.commons.storage;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileStorage<T> extends FileStorage<T> implements ArtifactStorage<T> {

	private static final String FILE_EXT = ".json";

	private final ObjectMapper mapper = new ObjectMapper();

	private final Class<T> entityType;

	public JsonFileStorage(Path storagePath, T emptyEntity, Class<T> entityType) {
		super(storagePath, emptyEntity);

		this.entityType = entityType;
	}

	@SuppressWarnings("unchecked")
	public JsonFileStorage(Path storagePath, T emptyEntity) {
		this(storagePath, emptyEntity, (Class<T>) emptyEntity.getClass());
	}

	@Override
	protected void write(Path dirPath, String id, T entity) throws IOException {
		mapper.writerWithDefaultPrettyPrinter().writeValue(toPath(dirPath, id).toFile(), entity);
	}

	@Override
	protected T read(Path dirPath, String id) throws IOException {
		Path path = toPath(dirPath, id);

		if (path.toFile().exists()) {
			return mapper.readValue(path.toFile(), entityType);
		} else {
			return null;
		}
	}

	@Override
	protected boolean remove(Path dirPath, String id) {
		return toPath(dirPath, id).toFile().delete();
	}

	private Path toPath(Path dirPath, String id) {
		return dirPath.resolve(id + FILE_EXT);
	}

}
