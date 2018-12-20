package org.continuity.idpa.application.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.entities.ApiFormats;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores application models in different versions in a folder. For versioning, the date when a model was
 * created is used.
 *
 * @author Henning Schulz
 *
 */
public class ApplicationModelRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationModelRepository.class);

	private static final String LEGACY_APPLICATION_FILE_NAME = "system.";
	private static final String APPLICATION_FILE_NAME = "application.";
	private static final String FILE_EXTENSION = ".yml";

	private static final DateFormat DATE_FORMAT = ApiFormats.DATE_FORMAT;

	private final IdpaYamlSerializer<Application> serializer;

	private final Path storagePath;

	public ApplicationModelRepository(String storagePath) {
		this(Paths.get(storagePath));
	}

	public ApplicationModelRepository(Path storagePath) {
		this(storagePath, new IdpaYamlSerializer<>(Application.class));
	}

	public ApplicationModelRepository(Path storagePath, IdpaYamlSerializer<Application> serializer) {
		this.storagePath = storagePath;
		this.serializer = serializer;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	/**
	 * Stores the specified application model with the specified tag.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param application
	 *            The application model.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public void save(String tag, Application application) throws IOException {
		Path path = getDirPath(tag).resolve(createFileName(application.getTimestamp()));
		serializer.writeToYaml(application, path);

		LOGGER.debug("Wrote application model to {}.", path);
	}

	private String createFileName(Date date) {
		return createFileName(APPLICATION_FILE_NAME, date);
	}

	private String createFileName(String applicationFileName, Date date) {
		return applicationFileName + DATE_FORMAT.format(date) + FILE_EXTENSION;
	}

	/**
	 * Reads the latest application model.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @return The latest application model.
	 */
	public Application readLatest(String tag) {
		for (ApplicationModelEntry entry : iterate(tag)) {
			return entry.getApplication();
		}

		return null;
	}

	/**
	 * Reads the latest application model that is older than the specified date.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param date
	 *            The date to compare with.
	 * @return A application model.
	 * @throws IOException
	 *             If an error during reading the application model occurs.
	 */
	public Application readLatestBefore(String tag, Date date) {
		for (ApplicationModelEntry entry : iterate(tag)) {
			if (!date.before(entry.getDate())) {
				return entry.getApplication();
			}
		}

		return null;
	}

	/**
	 * Reads the oldest application model that is newer than the specified date.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param date
	 *            The date to compare with.
	 * @return A application model.
	 * @throws IOException
	 *             If an error during reading the application model occurs.
	 */
	public Application readOldestAfter(String tag, Date date) {
		Application next = null;

		for (ApplicationModelEntry entry : iterate(tag)) {
			if (!date.before(entry.getDate())) {
				return next;
			}

			next = entry.getApplication();
		}

		return next;
	}

	/**
	 * Updates the timestamp of a application model. The new date is expected to be before the old date.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param oldTimestamp
	 *            The old timestamp.
	 * @param newTimestamp
	 *            The new timestamp.
	 *
	 * @throws IllegalArgumentException
	 *             If {@code newTimestamp} is after {@link oldTimestamp} or if there is no application
	 *             model at {@link oldTimestamp}.
	 * @throws IOException
	 *             If something goes wrong during changing the timestamp.
	 */
	public void updateApplicationChange(String tag, Date oldTimestamp, Date newTimestamp) throws IllegalArgumentException, IOException {
		if (!newTimestamp.before(oldTimestamp)) {
			throw new IllegalArgumentException("Cannot update application model with tag " + tag + " to date " + newTimestamp + "! This date is not before the original one: " + oldTimestamp);
		}

		Application system = readLatestBefore(tag, oldTimestamp);

		if (!oldTimestamp.equals(system.getTimestamp())) {
			throw new IllegalArgumentException("There is no application model with tag " + tag + " at date " + oldTimestamp + "!");
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
	 * Retrieves all stored legacy applications (system models) for a given tag.
	 *
	 * @param tag
	 *            The tag of the legacy applications.
	 * @return A list of strings representing the legacy applications.
	 * @throws NotDirectoryException
	 */
	public List<String> readLegacyApplications(String tag) throws NotDirectoryException {
		List<String> legacyApplications = new ArrayList<>();
		ApplicationIterator iterator = new ApplicationIterator(tag, LEGACY_APPLICATION_FILE_NAME);

		while (iterator.hasNext()) {
			legacyApplications.add(iterator.nextAsString());
		}

		return legacyApplications;
	}

	/**
	 * Returns an {@link Iterable} allowing to iterate over all application models in combination with
	 * the created date. The models are traversed in ascending order. That is, the newest model
	 * comes first.
	 *
	 * @param tag
	 *            The tag of the application models to be iterated.
	 * @return An iterator.
	 */
	public Iterable<ApplicationModelEntry> iterate(String tag) {
		return new ApplicationIterable(tag);
	}

	private class ApplicationIterable implements Iterable<ApplicationModelEntry> {

		private final String tag;

		public ApplicationIterable(String tag) {
			this.tag = tag;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<ApplicationModelEntry> iterator() {
			try {
				return new ApplicationIterator(tag);
			} catch (NotDirectoryException e) {
				LOGGER.error("Cannot iterate over application models of tag {}!", tag);
				return null;
			}
		}

	}

	private class ApplicationIterator implements Iterator<ApplicationModelEntry> {

		private final String tag;
		private final List<Date> dates;
		private final Iterator<Date> datesIterator;
		private final String applicationFileName;

		public ApplicationIterator(String tag) throws NotDirectoryException {
			this(tag, APPLICATION_FILE_NAME);
		}

		public ApplicationIterator(String tag, String applicationFileName) throws NotDirectoryException {
			this.tag = tag;
			Path dir = getDirPath(tag);
			this.applicationFileName = applicationFileName;
			this.dates = Arrays.stream(dir.toFile().list()).filter(name -> name.startsWith(applicationFileName)).map(this::extractDate).collect(Collectors.toList());
			Collections.sort(this.dates, Collections.reverseOrder());
			this.datesIterator = dates.iterator();
		}

		private Date extractDate(String deltaFileName) {
			String dateString = deltaFileName.substring(applicationFileName.length(), deltaFileName.length() - FILE_EXTENSION.length());
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
		public ApplicationModelEntry next() {
			Date date = datesIterator.next();
			String filename = createFileName(applicationFileName, date);
			try {
				return ApplicationModelEntry.of(ApplicationModelRepository.this, date, getDirPath(tag).resolve(filename));
			} catch (NotDirectoryException e) {
				LOGGER.error("Could not read application {} for tag {}! Returning null.", filename, tag);
				LOGGER.error("Expetion: ", e);
				return ApplicationModelEntry.of(ApplicationModelRepository.this, date, null);
			}
		}

		public String nextAsString() {
			Date date = datesIterator.next();
			String filename = createFileName(applicationFileName, date);

			try {
				return reduceLinesToString(Files.readAllLines(getDirPath(tag).resolve(filename)));
			} catch (IOException e) {
				LOGGER.error("Could not read legacy application {} for tag {}! Returning empty string.", filename, tag);
				LOGGER.error("Expetion: ", e);
				return "";
			}
		}

		private String reduceLinesToString(List<String> lines) {
			StringBuilder builder = new StringBuilder();

			lines.forEach(l -> {
				builder.append(l);
				builder.append("\n");
			});

			return builder.toString();
		}

	}

	/**
	 * Holds a application model in combination with the date when it was created.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class ApplicationModelEntry {

		private final IdpaYamlSerializer<Application> serializer;

		private final Path path;
		private final Date date;

		private ApplicationModelEntry(ApplicationModelRepository repository, Date date, Path path) {
			this.path = path;
			this.date = date;

			this.serializer = repository.serializer;
		}

		private static ApplicationModelEntry of(ApplicationModelRepository repository, Date date, Path path) {
			return new ApplicationModelEntry(repository, date, path);
		}

		public Date getDate() {
			return this.date;
		}

		public Application getApplication() {
			if (path == null) {
				return null;
			}

			try {
				return serializer.readFromYaml(path);
			} catch (IOException e) {
				LOGGER.error("Could not read application model from {}! Returning null.", path);
				e.printStackTrace();
				return null;
			}
		}

	}

}
