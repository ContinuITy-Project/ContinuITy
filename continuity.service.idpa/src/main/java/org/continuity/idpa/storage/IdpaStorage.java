package org.continuity.idpa.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores IDPAs in different versions in a folder structure. For versioning, the version or
 * timestamp when a model was created is used.
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

	private void onApplicationChanged(String tag, VersionOrTimestamp version) {
		listeners.forEach(l -> l.onApplicationChanged(tag, version));
	}

	private void onAnnotationChanged(String tag, VersionOrTimestamp version) {
		listeners.forEach(l -> l.onAnnotationChanged(tag, version));
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
		Path path = getDirPath(tag, application.getVersionOrTimestamp()).resolve(APPLICATION_FILE_NAME);
		appSerializer.writeToYaml(application, path);

		LOGGER.debug("Wrote application model to {}.", path);
		onApplicationChanged(tag, application.getVersionOrTimestamp());
	}

	/**
	 * Stores the specified annotation with the specified tag. Uses the version or timestamp of the
	 * annotation, which needs to be present.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param application
	 *            The application model.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public void save(String tag, ApplicationAnnotation annotation) throws IOException {
		if (annotation.getVersionOrTimestamp().isEmpty()) {
			throw new IllegalArgumentException("Cannot store an annotation without a version or timestamp! Either needs to be set or passed as assitional argument.");
		}

		save(tag, annotation.getVersionOrTimestamp(), annotation);
	}

	/**
	 * Stores the specified annotation with the specified tag.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param version
	 *            The version or timestamp of the annotation.
	 * @param application
	 *            The application model.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	private void save(String tag, VersionOrTimestamp version, ApplicationAnnotation annotation) throws IOException {
		Path path = getDirPath(tag, version).resolve(ANNOTATION_FILE_NAME);
		annSerializer.writeToYaml(annotation, path);

		LOGGER.debug("Wrote annotation model to {}.", path);
		onAnnotationChanged(tag, version);
	}

	/**
	 * Marks the annotation with the passed tag and version or timestamp to be broken.
	 *
	 * @param tag
	 * @param version
	 * @throws IOException
	 */
	public void markAsBroken(String tag, VersionOrTimestamp version) throws IOException {
		Path path = getDirPath(tag, version).resolve(BROKEN_FILE_NAME);
		Files.write(path, Collections.singletonList(BROKEN_CONTENT), StandardOpenOption.CREATE);
	}

	/**
	 * Removes a potentially existing mark of the annotation with the passed tag and version or
	 * timestamp to be broken.
	 *
	 * @param tag
	 * @param version
	 * @return {@code true} if there was a mark or {@code false} otherwise.
	 * @throws IOException
	 */
	public boolean unmarkAsBroken(String tag, VersionOrTimestamp version) throws IOException {
		Path path = getDirPath(tag, version).resolve(BROKEN_FILE_NAME);
		return Files.deleteIfExists(path);
	}

	/**
	 * Returns whether the annotation with the passed tag and version or timestamp is marked as
	 * broken.
	 *
	 * @param tag
	 * @return
	 * @throws NotDirectoryException
	 */
	public boolean isBroken(String tag, VersionOrTimestamp version) {
		return readLatestBefore(tag, version).checkAdditionalFlag(FLAG_BROKEN);
	}

	/**
	 * Reads the latest IDPA.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @return An <b>immutable</b> IDPA.
	 */
	public Idpa readLatest(String tag) {
		return readLatestBefore(tag, VersionOrTimestamp.MAX_VALUE);
	}

	/**
	 * Reads the latest IDPA that is older than the specified version.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @param version
	 *            The version to compare with.
	 * @return An <b>immutable</b> IDPA.
	 * @throws IOException
	 *             If an error during reading the IDPA occurs.
	 */
	public Idpa readLatestBefore(String tag, VersionOrTimestamp version) {
		for (IdpaEntry entry : iterate(tag)) {
			if (!version.before(entry.getVersionOrTimestamp())) {
				return entry;
			}
		}

		return null;
	}

	/**
	 * Reads the oldest IDPA that is newer than the specified version.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @param version
	 *            The version to compare with.
	 * @return An <b>immutable</b> IDPA.
	 * @throws IOException
	 *             If an error during reading the IDPA occurs.
	 */
	public Idpa readOldestAfter(String tag, VersionOrTimestamp version) {
		IdpaEntry next = null;

		for (IdpaEntry entry : iterate(tag)) {
			if (!version.before(entry.getVersionOrTimestamp())) {
				return next;
			}

			next = entry;
		}

		return next;
	}

	/**
	 * Updates the version or timestamp of a application model. The new version is expected to be
	 * before the old version.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param oldVersion
	 *            The old version or timestamp.
	 * @param newVersion
	 *            The new version or timestamp.
	 *
	 * @throws IllegalArgumentException
	 *             If {@code newVersion} is after {@link oldVersion} or if there is no application
	 *             model at {@link oldVersion}.
	 * @throws IOException
	 *             If something goes wrong during changing the version or timestamp.
	 */
	public void updateApplicationChange(String tag, VersionOrTimestamp oldVersion, VersionOrTimestamp newVersion) throws IllegalArgumentException, IOException {
		if (!newVersion.before(oldVersion)) {
			throw new IllegalArgumentException("Cannot update application model with tag " + tag + " to version " + newVersion + "! This version is not before the original one: " + oldVersion);
		}

		Idpa idpa = readLatestBefore(tag, oldVersion);
		Application application = idpa.getApplication();

		if (!oldVersion.equals(application.getVersionOrTimestamp())) {
			throw new IllegalArgumentException("There is no application model with tag " + tag + " at version " + oldVersion + "!");
		}

		application.setVersionOrTimestamp(newVersion);
		save(tag, application);

		if (idpa.getAnnotation() != null) {
			save(tag, newVersion, idpa.getAnnotation());
		}

		delete(tag, oldVersion);
	}

	private void delete(String tag, VersionOrTimestamp version) throws IOException {
		FileUtils.deleteDirectory(getDirPath(tag).resolve(version.toString()).toFile());
	}

	private Path getDirPath(String tag) throws NotDirectoryException {
		Path dirPath = storagePath.resolve(tag);
		checkAndCreateDirs(dirPath, true);
		return dirPath;
	}

	private Path getDirPath(String tag, VersionOrTimestamp version) throws NotDirectoryException {
		return getDirPath(tag, version, true);
	}

	private Path getDirPath(String tag, VersionOrTimestamp version, boolean createDirs) throws NotDirectoryException {
		Path dirPath = getDirPath(tag).resolve(version.toString());
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
	 * created version. The models are traversed in descending order. That is, the newest model
	 * comes first.
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
		private final Iterator<VersionOrTimestamp> versionIterator;
		private final Map<VersionOrTimestamp, Path> appPerVersion;

		public ApplicationIterator(String tag) throws NotDirectoryException {
			this.tag = tag;

			Path dir = getDirPath(tag);
			List<VersionOrTimestamp> versions = Arrays.stream(dir.toFile().list()).filter(d -> !d.startsWith(".")).map(this::extractVersion).filter(Objects::nonNull).collect(Collectors.toList());
			Collections.sort(versions);
			this.appPerVersion = findApplicationPerVersion(dir, versions);

			Collections.reverse(versions);
			this.versionIterator = versions.iterator();
		}

		private VersionOrTimestamp extractVersion(String string) {
			try {
				return VersionOrTimestamp.fromString(string);
			} catch (ParseException e) {
				LOGGER.warn("Could not parse version {}! Ignoring the version.", string);
			}

			return null;
		}

		private Map<VersionOrTimestamp, Path> findApplicationPerVersion(Path dir, List<VersionOrTimestamp> versions) {
			Map<VersionOrTimestamp, Path> appPerVersion = new HashMap<>();
			VersionOrTimestamp versionOfLastApp = null;

			for (VersionOrTimestamp v : versions) {
				if (dir.resolve(v.toString()).resolve(APPLICATION_FILE_NAME).toFile().exists()) {
					versionOfLastApp = v;
				}

				appPerVersion.put(v, dir.resolve(versionOfLastApp.toString()));
			}

			return appPerVersion;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return versionIterator.hasNext();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IdpaEntry next() {
			VersionOrTimestamp version = versionIterator.next();
			String folder = version.toString();

			Path path;
			try {
				path = getDirPath(tag).resolve(folder);
			} catch (NotDirectoryException e) {
				LOGGER.error("Could not read application {} for tag {}! Returning null.", folder, tag);
				LOGGER.error("Exception: ", e);
				return null;
			}

			IdpaEntry entry = IdpaEntry.of(IdpaStorage.this, version, path);
			entry.setAppPath(appPerVersion.get(entry.getVersionOrTimestamp()));

			return entry;
		}

	}

	/**
	 * Holds a application model in combination with the version when it was created.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaEntry extends Idpa {

		private final IdpaYamlSerializer<Application> appSerializer;

		private final IdpaYamlSerializer<ApplicationAnnotation> annSerializer;

		private Path appPath;
		private Path annPath;

		private IdpaEntry(IdpaStorage storage, VersionOrTimestamp version, Path path) {
			this.appPath = path;
			this.annPath = path;

			this.appSerializer = storage.appSerializer;
			this.annSerializer = storage.annSerializer;

			this.setVersionOrTimestamp(version);
		}

		private static IdpaEntry of(IdpaStorage storage, VersionOrTimestamp version, Path path) {
			return new IdpaEntry(storage, version, path);
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
