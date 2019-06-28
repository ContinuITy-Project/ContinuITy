package org.continuity.commons.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stores artifacts on the file storages based on app-ids.
 *
 * @author Henning Schulz
 *
 */
public class AppIdFileStorage<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppIdFileStorage.class);

	private static final String FILE_EXT = ".json";

	private final ObjectMapper mapper = new ObjectMapper();

	private final Path storagePath;

	private final Class<T> artifactType;

	public AppIdFileStorage(Path storagePath, Class<T> artifactType) {
		this.storagePath = storagePath;
		storagePath.toFile().mkdirs();

		this.artifactType = artifactType;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	public void store(T artifact, AppId aid) throws IOException {
		mapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.resolve(aid + FILE_EXT).toFile(), artifact);
	}

	public T read(AppId aid) throws IOException {
		File file = storagePath.resolve(aid + FILE_EXT).toFile();

		if (file.exists()) {
			return mapper.readValue(file, artifactType);
		} else {
			return null;
		}
	}

}
