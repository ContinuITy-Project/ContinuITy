package org.continuity.idpa.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.continuity.api.entities.ApiFormats;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores IDPAs in different versions in a folder structure. For versioning, the date when a model
 * was created is used.
 *
 * @author Henning Schulz
 *
 */
public class IdpaStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdpaStorage.class);

	public static final String FLAG_BROKEN = "IdpaStorage.BROKEN";

	private static final String APPLICATION_FILE_NAME = "application.yml";
	private static final String ANNOTATION_FILE_NAME = "annotation.yml";

	private static final String BROKEN_FILE_NAME = "broken.txt";
	private static final String BROKEN_CONTENT = "This annotation is broken";

	private static final DateFormat DATE_FORMAT = ApiFormats.DATE_FORMAT;

	private final IdpaYamlSerializer<Application> appSerializer;
	private final IdpaYamlSerializer<ApplicationAnnotation> annSerializer;

	private final Path storagePath;

	private final List<IdpaStorageListener> listeners = new ArrayList<>();

	public IdpaStorage(String storagePath) {
		this(Paths.get(storagePath));
	}

	public IdpaStorage(Path storagePath) {
		this(storagePath, new IdpaYamlSerializer<>(Application.class), new IdpaYamlSerializer<>(ApplicationAnnotation.class));
	}

	public IdpaStorage(Path storagePath, IdpaYamlSerializer<Application> appSerializer, IdpaYamlSerializer<ApplicationAnnotation> annSerializer) {
		this.storagePath = storagePath;
		this.appSerializer = appSerializer;
		this.annSerializer = annSerializer;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	/**
	 * Adds a listener that will be notified whenever an application or annotation has been changed.
	 *
	 * @param listener
	 *            An {@link IdpaStorageListener}.
	 */
	public void registerListener(IdpaStorageListener listener) {
		this.listeners.add(listener);
	}

	private void onApplicationChanged(String tag, Date timestamp) {
		listeners.forEach(l -> l.onApplicationChanged(tag, timestamp));
	}

	private void onAnnotationChanged(String tag, Date timestamp) {
		listeners.forEach(l -> l.onAnnotationChanged(tag, timestamp));
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
		Path path = getDirPath(tag, application.getTimestamp()).resolve(APPLICATION_FILE_NAME);
		appSerializer.writeToYaml(application, path);

		LOGGER.debug("Wrote application model to {}.", path);
		onApplicationChanged(tag, application.getTimestamp());
	}

	/**
	 * Stores the specified annoation with the specified tag.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param application
	 *            The application model.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public void save(String tag, Date timestamp, ApplicationAnnotation annotation) throws IOException {
		Path path = getDirPath(tag, timestamp).resolve(ANNOTATION_FILE_NAME);
		annSerializer.writeToYaml(annotation, path);

		LOGGER.debug("Wrote annotation model to {}.", path);
		onAnnotationChanged(tag, timestamp);
	}

	/**
	 * Marks the annotation with the passed tag and timestamp to be broken.
	 *
	 * @param tag
	 * @param timestamp
	 * @throws IOException
	 */
	public void markAsBroken(String tag, Date timestamp) throws IOException {
		Path path = getDirPath(tag, timestamp).resolve(BROKEN_FILE_NAME);
		Files.write(path, Collections.singletonList(BROKEN_CONTENT), StandardOpenOption.CREATE);
	}

	/**
	 * Removes a potentially existing mark of the annotation with the passed tag and timestamp to be
	 * broken.
	 *
	 * @param tag
	 * @param timestamp
	 * @return {@code true} if there was a mark or {@code false} otherwise.
	 * @throws IOException
	 */
	public boolean unmarkAsBroken(String tag, Date timestamp) throws IOException {
		Path path = getDirPath(tag, timestamp).resolve(BROKEN_FILE_NAME);
		return Files.deleteIfExists(path);
	}

	/**
	 * Returns whether the annotation with the passed tag and timestamp is marked as broken.
	 *
	 * @param tag
	 * @return
	 * @throws NotDirectoryException
	 */
	public boolean isBroken(String tag, Date timestamp) {
		return readLatestBefore(tag, timestamp).checkAdditionalFlag(FLAG_BROKEN);
	}

	/**
	 * Reads the latest IDPA.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @return An <b>immutable</b> IDPA.
	 */
	public Idpa readLatest(String tag) {
		return readLatestBefore(tag, new Date(Long.MAX_VALUE));
	}

	/**
	 * Reads the latest IDPA that is older than the specified date.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @param date
	 *            The date to compare with.
	 * @return An <b>immutable</b> IDPA.
	 * @throws IOException
	 *             If an error during reading the IDPA occurs.
	 */
	public Idpa readLatestBefore(String tag, Date date) {
		for (IdpaEntry entry : iterate(tag)) {
			if (!date.before(entry.getTimestamp())) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * Reads the oldest IDPA that is newer than the specified date.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @param date
	 *            The date to compare with.
	 * @return An <b>immutable</b> IDPA.
	 * @throws IOException
	 *             If an error during reading the IDPA occurs.
	 */
	public Idpa readOldestAfter(String tag, Date date) {
		IdpaEntry next = null;

		for (IdpaEntry entry : iterate(tag)) {
			if (!date.before(entry.getTimestamp())) {
				return next;
			}

			next = entry;
		}

		return next;
	}

	/**
	 * Updates the timestamp of a application model. The new date is expected to be before the old
	 * date.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param oldTimestamp
	 *            The old timestamp.
	 * @param newTimestamp
	 *            The new timestamp.
	 *
	 * @throws IllegalArgumentException
	 *             If {@code newTimestamp} is after {@link oldTimestamp} or if there is no
	 *             application model at {@link oldTimestamp}.
	 * @throws IOException
	 *             If something goes wrong during changing the timestamp.
	 */
	public void updateApplicationChange(String tag, Date oldTimestamp, Date newTimestamp) throws IllegalArgumentException, IOException {
		if (!newTimestamp.before(oldTimestamp)) {
			throw new IllegalArgumentException("Cannot update application model with tag " + tag + " to date " + newTimestamp + "! This date is not before the original one: " + oldTimestamp);
		}

		Idpa idpa = readLatestBefore(tag, oldTimestamp);
		Application application = idpa.getApplication();

		if (!oldTimestamp.equals(application.getTimestamp())) {
			throw new IllegalArgumentException("There is no application model with tag " + tag + " at date " + oldTimestamp + "!");
		}

		application.setTimestamp(newTimestamp);
		save(tag, application);

		if (idpa.getAnnotation() != null) {
			save(tag, newTimestamp, idpa.getAnnotation());
		}

		delete(tag, oldTimestamp);
	}

	private void delete(String tag, Date date) throws IOException {
		FileUtils.deleteDirectory(getDirPath(tag).resolve(DATE_FORMAT.format(date)).toFile());
	}

	private Path getDirPath(String tag) throws NotDirectoryException {
		Path dirPath = storagePath.resolve(tag);
		checkAndCreateDirs(dirPath, true);
		return dirPath;
	}

	private Path getDirPath(String tag, Date timestamp) throws NotDirectoryException {
		return getDirPath(tag, timestamp, true);
	}

	private Path getDirPath(String tag, Date timestamp, boolean createDirs) throws NotDirectoryException {
		Path dirPath = getDirPath(tag).resolve(DATE_FORMAT.format(timestamp));
		checkAndCreateDirs(dirPath, createDirs);
		return dirPath;
	}

	private void checkAndCreateDirs(Path dirPath, boolean create) throws NotDirectoryException {
		File dir = dirPath.toFile();

		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.error("{} is not a directory!", dir.getAbsolutePath());
			throw new NotDirectoryException(dir.getAbsolutePath());
		}

		if (create) {
			dir.mkdirs();
		}
	}

	/**
	 * Returns an {@link Iterable} allowing to iterate over all IDPAs in combination with the
	 * created date. The models are traversed in descending order. That is, the newest model comes
	 * first.
	 *
	 * @param tag
	 *            The tag of the application models to be iterated.
	 * @return An iterator.
	 */
	public Iterable<IdpaEntry> iterate(String tag) {
		return new ApplicationIterable(tag);
	}

	private class ApplicationIterable implements Iterable<IdpaEntry> {

		private final String tag;

		public ApplicationIterable(String tag) {
			this.tag = tag;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<IdpaEntry> iterator() {
			try {
				return new ApplicationIterator(tag);
			} catch (NotDirectoryException e) {
				LOGGER.error("Cannot iterate over application models of tag {}!", tag);
				return null;
			}
		}

	}

	private class ApplicationIterator implements Iterator<IdpaEntry> {

		private final String tag;
		private final Iterator<Date> datesIterator;
		private final Map<Date, Path> appPerDate;

		public ApplicationIterator(String tag) throws NotDirectoryException {
			this.tag = tag;

			Path dir = getDirPath(tag);
			List<Date> dates = Arrays.stream(dir.toFile().list()).filter(d -> !d.startsWith(".")).map(this::extractDate).filter(Objects::nonNull).collect(Collectors.toList());
			Collections.sort(dates);
			this.appPerDate = findApplicationPerDate(dir, dates);

			Collections.reverse(dates);
			this.datesIterator = dates.iterator();
		}

		private Date extractDate(String dateString) {
			try {
				return DATE_FORMAT.parse(dateString);
			} catch (ParseException e) {
				LOGGER.warn("Could not parse date {}! Ignoring the version.", dateString);
			}

			return null;
		}

		private Map<Date, Path> findApplicationPerDate(Path dir, List<Date> dates) {
			Map<Date, Path> appPerDate = new HashMap<>();
			Date dateOfLastApp = null;

			for (Date d : dates) {
				if (dir.resolve(DATE_FORMAT.format(d)).resolve(APPLICATION_FILE_NAME).toFile().exists()) {
					dateOfLastApp = d;
				}

				appPerDate.put(d, dir.resolve(DATE_FORMAT.format(dateOfLastApp)));
			}

			return appPerDate;
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
		public IdpaEntry next() {
			Date date = datesIterator.next();
			String folder = DATE_FORMAT.format(date);

			Path path;
			try {
				path = getDirPath(tag).resolve(folder);
			} catch (NotDirectoryException e) {
				LOGGER.error("Could not read application {} for tag {}! Returning null.", folder, tag);
				LOGGER.error("Exception: ", e);
				return null;
			}

			IdpaEntry entry = IdpaEntry.of(IdpaStorage.this, date, path);
			entry.setAppPath(appPerDate.get(entry.getTimestamp()));

			return entry;
		}

	}

	/**
	 * Holds a application model in combination with the date when it was created.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaEntry extends Idpa {

		private final IdpaYamlSerializer<Application> appSerializer;

		private final IdpaYamlSerializer<ApplicationAnnotation> annSerializer;

		private Path appPath;
		private Path annPath;

		private IdpaEntry(IdpaStorage storage, Date date, Path path) {
			this.appPath = path;
			this.annPath = path;

			this.appSerializer = storage.appSerializer;
			this.annSerializer = storage.annSerializer;

			this.setTimestamp(date);
		}

		private static IdpaEntry of(IdpaStorage storage, Date date, Path path) {
			return new IdpaEntry(storage, date, path);
		}

		@Override
		public Application getApplication() {
			if (appPath == null) {
				return null;
			}

			try {
				return appSerializer.readFromYaml(appPath.resolve(APPLICATION_FILE_NAME));
			} catch (IOException e) {
				LOGGER.error("Could not read application model from {}! Returning null.", appPath);
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			if (!hasAnnotation()) {
				return null;
			}

			try {
				return annSerializer.readFromYaml(annPath.resolve(ANNOTATION_FILE_NAME));
			} catch (IOException e) {
				LOGGER.error("Could not read annotation from {}! Returning null.", annPath);
				e.printStackTrace();
				return null;
			}
		}

		public boolean hasAnnotation() {
			return (annPath != null) && annPath.resolve(ANNOTATION_FILE_NAME).toFile().exists();
		}

		public Path getAppPath() {
			return appPath;
		}

		public Path getAnnPath() {
			return annPath;
		}

		public void setAppPath(Path path) {
			this.appPath = path;
		}

		public void setAnnPath(Path path) {
			this.annPath = path;
		}

		public boolean isBroken() {
			return Files.exists(annPath.resolve(BROKEN_FILE_NAME));
		}

		@Override
		public boolean checkAdditionalFlag(String key) {
			if (FLAG_BROKEN.equals(key)) {
				return isBroken();
			}

			return super.checkAdditionalFlag(key);
		}

		@Override
		public void setApplication(Application application) {
			throw new UnsupportedOperationException("Cannot set the application of an IdpaStorage.IdpaEntry!");
		}

		@Override
		public void setAnnotation(ApplicationAnnotation annotation) {
			throw new UnsupportedOperationException("Cannot set the annotation of an IdpaStorage.IdpaEntry!");
		}

	}

}
