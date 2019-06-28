package org.continuity.commons.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.continuity.idpa.AppId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores artifacts in the file system.
 *
 * @author Henning Schulz
 *
 * @param <T>
 *            The type of the stored artifacts.
 */
public abstract class FileStorage<T> implements ArtifactStorage<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileStorage.class);

	private static final String DELIM = "-";

	private static final String FILE_PATTERN = "^([0-9]+).*";

	private final Path storagePath;

	private final T emptyEntity;

	public FileStorage(Path storagePath, T emptyEntity) {
		this.storagePath = storagePath;
		storagePath.toFile().mkdirs();

		this.emptyEntity = emptyEntity;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	/**
	 * Writes the artifacts to the file system. Implementations should store files or directories
	 * that start with the passed IDs.
	 *
	 * @param dirPath
	 *            The directory in which the artifact should be stored.
	 * @param id
	 *            The ID of the artifact.
	 * @param entity
	 *            The artifact to be stored.
	 * @throws IOException
	 *             If writing fails.
	 */
	protected abstract void write(Path dirPath, String id, T entity) throws IOException;

	/**
	 * Reads an artifact.
	 *
	 * @param dirPath
	 * @param id
	 * @return
	 * @throws IOException
	 */
	protected abstract T read(Path dirPath, String id) throws IOException;

	/**
	 * Removes an artifact.
	 *
	 * @param dirPath
	 * @param id
	 * @return {@code true} if the artifact existed and was removed successfully.
	 * @throws IOException
	 */
	protected abstract boolean remove(Path dirPath, String id) throws IOException;

	@Override
	public String reserve(AppId aid) {
		return put(emptyEntity, aid);
	}

	@Override
	public void putToReserved(String id, T entity) {
		store(id, entity);
	}

	@Override
	public String put(T entity, AppId aid) {
		String id = getNextNumber() + DELIM + aid;

		store(id, entity);

		return id;
	}

	private void store(String id, T entity) {
		try {
			write(storagePath, id, entity);
		} catch (IOException e) {
			LOGGER.error("Error during writing!", e);
		}
	}

	@Override
	public T get(String id) {
		try {
			return read(storagePath, id);
		} catch (IOException e) {
			LOGGER.error("Error during reading!", e);
			return null;
		}
	}

	@Override
	public boolean remove(String id) {
		try {
			return remove(storagePath, id);
		} catch (IOException e) {
			LOGGER.error("Error during deleting!", e);
			return false;
		}
	}

	@Override
	public AppId getAppIdForId(String id) {
		return AppId.fromString(id.substring(id.indexOf(DELIM) + 1));
	}

	private int getNextNumber() {
		int max = 0;

		for (String file : storagePath.toFile().list()) {
			if (file.matches(FILE_PATTERN)) {
				max = Math.max(max, getNumber(file));
			}
		}

		return max + 1;
	}

	private int getNumber(String filename) {
		Pattern pattern = Pattern.compile(FILE_PATTERN);
		Matcher matcher = pattern.matcher(filename);

		if (matcher.find()) {
			return Integer.valueOf(matcher.group(1));
		} else {
			return -1;
		}
	}

}
