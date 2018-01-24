package org.continuity.system.model.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.yaml.ContinuityYamlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores system models in different versions in a folder. For versioning, the date when a model was
 * created is used.
 *
 * @author Henning Schulz
 *
 */
public class SystemModelRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(SystemModelRepository.class);

	private static final String SYSTEM_MODEL_FILE_NAME = "system.";
	private static final String FILE_EXTENSION = ".yml";

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss-SSS");

	private final ContinuityYamlSerializer<SystemModel> serializer;

	private final Path storagePath;

	public SystemModelRepository(String storagePath) {
		this(Paths.get(storagePath));
	}

	public SystemModelRepository(Path storagePath) {
		this(storagePath, new ContinuityYamlSerializer<>(SystemModel.class));
	}

	public SystemModelRepository(Path storagePath, ContinuityYamlSerializer<SystemModel> serializer) {
		this.storagePath = storagePath;
		this.serializer = serializer;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	/**
	 * Stores the specified system model with the specified tag.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param systemModel
	 *            The system model.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public void save(String tag, SystemModel systemModel) throws IOException {
		Path path = getDirPath(tag).resolve(createFileName(systemModel.getTimestamp()));
		serializer.writeToYaml(systemModel, path);

		LOGGER.debug("Wrote system model to {}.", path);
	}

	private String createFileName(Date date) {
		return SYSTEM_MODEL_FILE_NAME + DATE_FORMAT.format(date) + FILE_EXTENSION;
	}

	/**
	 * Reads the latest system model.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @return The latest system model.
	 */
	public SystemModel readLatest(String tag) {
		for (SystemModelEntry entry : iterate(tag)) {
			return entry.getSystemModel();
		}

		return null;
	}

	/**
	 * Reads the latest system model that is older than the specified date.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param date
	 *            The date to compare with.
	 * @return A system model.
	 * @throws IOException
	 *             If an error during reading the system model occurs.
	 */
	public SystemModel readLatestBefore(String tag, Date date) {
		for (SystemModelEntry entry : iterate(tag)) {
			if (!date.before(entry.getDate())) {
				return entry.getSystemModel();
			}
		}

		return null;
	}

	/**
	 * Reads the oldest system model that is newer than the specified date.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param date
	 *            The date to compare with.
	 * @return A system model.
	 * @throws IOException
	 *             If an error during reading the system model occurs.
	 */
	public SystemModel readOldestAfter(String tag, Date date) {
		SystemModel next = null;

		for (SystemModelEntry entry : iterate(tag)) {
			if (!date.before(entry.getDate())) {
				return next;
			}

			next = entry.getSystemModel();
		}

		return next;
	}

	/**
	 * Updates the timestamp of a system model. The new date is expected to be before the old date.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param oldTimestamp
	 *            The old timestamp.
	 * @param newTimestamp
	 *            The new timestamp.
	 *
	 * @throws IllegalArgumentException
	 *             If {@code newTimestamp} is after {@link oldTimestamp} or if there is no system
	 *             model at {@link oldTimestamp}.
	 * @throws IOException
	 *             If something goes wrong during changing the timestamp.
	 */
	public void updateSystemChange(String tag, Date oldTimestamp, Date newTimestamp) throws IllegalArgumentException, IOException {
		if (!newTimestamp.before(oldTimestamp)) {
			throw new IllegalArgumentException("Cannot update system model with tag " + tag + " to date " + newTimestamp + "! This date is not before the original one: " + oldTimestamp);
		}

		SystemModel system = readLatestBefore(tag, oldTimestamp);

		if (!oldTimestamp.equals(system.getTimestamp())) {
			throw new IllegalArgumentException("There is no system model with tag " + tag + " at date " + oldTimestamp + "!");
		}

		system.setTimestamp(newTimestamp);
		save(tag, system);
		delete(tag, oldTimestamp);
	}

	private boolean delete(String tag, Date date) throws NotDirectoryException {
		return getDirPath(tag).resolve(createFileName(date)).toFile().delete();
	}

	private Path getDirPath(String tag) throws NotDirectoryException {
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();

		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.error("{} is not a directory!", dir.getAbsolutePath());
			throw new NotDirectoryException(dir.getAbsolutePath());
		}

		dir.mkdirs();

		return dirPath;
	}

	/**
	 * Returns an {@link Iterable} allowing to iterate over all system models in combination with
	 * the created date. The models are traversed in ascending order. That is, the newest model
	 * comes first.
	 *
	 * @param tag
	 *            The tag of the system models to be iterated.
	 * @return An iterator.
	 */
	public Iterable<SystemModelEntry> iterate(String tag) {
		return new SystemModelIterable(tag);
	}

	private class SystemModelIterable implements Iterable<SystemModelEntry> {

		private final String tag;

		public SystemModelIterable(String tag) {
			this.tag = tag;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<SystemModelEntry> iterator() {
			try {
				return new SystemModelIterator(tag);
			} catch (NotDirectoryException e) {
				LOGGER.error("Cannot iterate over system models of tag {}!", tag);
				return null;
			}
		}

	}

	private class SystemModelIterator implements Iterator<SystemModelEntry> {

		private final String tag;
		private final List<Date> dates;
		private final Iterator<Date> datesIterator;

		public SystemModelIterator(String tag) throws NotDirectoryException {
			this.tag = tag;
			Path dir = getDirPath(tag);
			this.dates = Arrays.stream(dir.toFile().list()).filter(name -> name.startsWith(SYSTEM_MODEL_FILE_NAME)).map(this::extractDate).collect(Collectors.toList());
			Collections.sort(this.dates, Collections.reverseOrder());
			this.datesIterator = dates.iterator();
		}

		private Date extractDate(String deltaFileName) {
			String dateString = deltaFileName.substring(7, deltaFileName.length() - 4);
			try {
				return DATE_FORMAT.parse(dateString);
			} catch (ParseException e) {
				LOGGER.error("Could not parse date {}! Returning 1990/01/01", dateString);
				e.printStackTrace();
			}

			return new Date(0);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return datesIterator.hasNext();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public SystemModelEntry next() {
			Date date = datesIterator.next();
			String filename = createFileName(date);
			try {
				return SystemModelEntry.of(SystemModelRepository.this, date, getDirPath(tag).resolve(filename));
			} catch (NotDirectoryException e1) {
				LOGGER.error("Could not read delta {} for tag {}! Returning null.", filename, tag);
				e1.printStackTrace();
				return SystemModelEntry.of(SystemModelRepository.this, date, null);
			}
		}

	}

	/**
	 * Holds a system model in combination with the date when it was created.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class SystemModelEntry {

		private final ContinuityYamlSerializer<SystemModel> serializer;

		private final Path path;
		private final Date date;

		private SystemModelEntry(SystemModelRepository repository, Date date, Path path) {
			this.path = path;
			this.date = date;

			this.serializer = repository.serializer;
		}

		private static SystemModelEntry of(SystemModelRepository repository, Date date, Path path) {
			return new SystemModelEntry(repository, date, path);
		}

		public Date getDate() {
			return this.date;
		}

		public SystemModel getSystemModel() {
			if (path == null) {
				return null;
			}

			try {
				return serializer.readFromYaml(path);
			} catch (IOException e) {
				LOGGER.error("Could not read system model from {}! Returning null.", path);
				e.printStackTrace();
				return null;
			}
		}

	}

}
