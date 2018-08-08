package org.continuity.commons.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Stores artifacts on the file storages based on tags.
 *
 * @author Henning Schulz
 *
 */
public class TagFileStorage<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TagFileStorage.class);

	private static final String FILE_EXT = ".json";

	private final ObjectMapper mapper = new ObjectMapper();

	private final Path storagePath;

	private final Class<T> artifactType;

	public TagFileStorage(Path storagePath, Class<T> artifactType) {
		this.storagePath = storagePath;
		storagePath.toFile().mkdirs();

		this.artifactType = artifactType;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	public void store(T artifact, String tag) throws IOException {
		mapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.resolve(tag + FILE_EXT).toFile(), artifact);
	}

	public T read(String tag) throws IOException {
		File file = storagePath.resolve(tag + FILE_EXT).toFile();

		if (file.exists()) {
			return mapper.readValue(file, artifactType);
		} else {
			return null;
		}
	}

}
