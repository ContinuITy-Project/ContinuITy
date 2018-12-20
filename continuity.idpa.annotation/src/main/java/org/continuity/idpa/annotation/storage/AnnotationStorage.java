package org.continuity.idpa.annotation.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.serialization.yaml.IdpaYamlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationStorage.class);

	private static final String LEGACY_APPLICATION_FILE_NAME = "system";
	private static final String APPLICATION_FILE_NAME = "application";
	private static final String ANNOTATION_FILE_NAME = "annotation";
	private static final String FILE_EXTENSION = ".yml";

	private static final String BROKEN_FILE_NAME = "broken.txt";
	private static final String BROKEN_CONTENT = "This annotation is broken";

	private final Path storagePath;

	public AnnotationStorage(String storagePath) {
		this(Paths.get(storagePath));
	}

	public AnnotationStorage(Path storagePath) {
		this.storagePath = storagePath;

		LOGGER.info("Using storage path {}.", storagePath.toAbsolutePath());
	}

	/**
	 * Stores the specified models with the specified tag. If there are already models with the same
	 * tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the models.
	 * @param applicationModel
	 *            The application model.
	 * @param annotation
	 *            The annotation
	 * @param suffix
	 *            A suffix to be appended to the file name of the annotation.
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, Application applicationModel, ApplicationAnnotation annotation, String suffix) throws IOException {
		boolean created = saveOrUpdate(tag, applicationModel);
		created = created || saveOrUpdate(tag, annotation, suffix);

		return !created;
	}

	/**
	 * Stores the specified models with the specified tag. If there are already models with the same
	 * tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the models.
	 * @param applicationModel
	 *            The application model.
	 * @param annotation
	 *            The annotation
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, Application applicationModel, ApplicationAnnotation annotation) throws IOException {
		return saveOrUpdate(tag, applicationModel, annotation, null);
	}

	/**
	 * Stores the specified application model with the specified tag. If there are already models
	 * with the same tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param applicationModel
	 *            The application model.
	 * @param suffix
	 *            A suffix to be appended to the file name.
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, Application applicationModel, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, false);

		if (dirPath == null) {
			throw new NotDirectoryException(storagePath.resolve(tag).toAbsolutePath().toString());
		}

		boolean created = dirPath.toFile().mkdirs();

		String filename = APPLICATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);
		serializer.writeToYaml(applicationModel, dirPath.resolve(filename));

		LOGGER.debug("Wrote application model to {}.", dirPath);

		return !created;
	}

	/**
	 * Stores the specified application model with the specified tag. If there are already models
	 * with the same tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param applicationModel
	 *            The application model.
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, Application applicationModel) throws IOException {
		return saveOrUpdate(tag, applicationModel, (String) null);
	}

	/**
	 * Stores the specified annotation with the specified tag. If there are already models with the
	 * same tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param annotation
	 *            The annotation
	 * @param suffix
	 *            A suffix to be appended to the file name.
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, ApplicationAnnotation annotation, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, false);

		if (dirPath == null) {
			throw new NotDirectoryException(storagePath.resolve(tag).toAbsolutePath().toString());
		}

		boolean created = dirPath.toFile().mkdirs();

		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

		String filename = ANNOTATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		serializer.writeToYaml(annotation, dirPath.resolve(filename));

		LOGGER.debug("Wrote annotation to {}.", dirPath);

		return !created;
	}

	/**
	 * Stores the specified annotation with the specified tag. If there are already models with the
	 * same tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param annotation
	 *            The annotation
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, ApplicationAnnotation annotation) throws IOException {
		return saveOrUpdate(tag, annotation, (String) null);
	}

	/**
	 * Stores the specified annotation with the specified tag if there is no such annotation.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param annotation
	 *            The annotation
	 * @return {@code true} if and only if the annotation was stored.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveIfNotPresent(String tag, ApplicationAnnotation annotation) throws IOException {
		return saveIfNotPresent(tag, annotation, null);
	}

	/**
	 * Stores the specified annotation with the specified tag if there is no such annotation.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param annotation
	 *            The annotation
	 * @param suffix
	 *            A suffix to be appended to the filename.
	 * @return {@code true} if and only if the annotation was stored.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveIfNotPresent(String tag, ApplicationAnnotation annotation, String suffix) throws IOException {
		Path dirPath = getDir(tag, true, false);

		if (dirPath == null) {
			throw new NotDirectoryException(storagePath.resolve(tag).toAbsolutePath().toString());
		}

		String filename = ANNOTATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		Path annotationPath = dirPath.resolve(filename);
		boolean exists = new File(annotationPath.toString()).exists();

		if (!exists) {
			IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);
			serializer.writeToYaml(annotation, annotationPath);

			LOGGER.debug("Wrote annotation to {}.", dirPath);
		} else {
			LOGGER.debug("Did not write annotation. There was already one at {}.", dirPath);
		}

		return !exists;
	}

	/**
	 * Stores the specified application model with the specified tag if there is no such model.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param application
	 *            The application model.
	 * @return {@code true} if and only if the application model was stored.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveIfNotPresent(String tag, Application application) throws IOException {
		return saveIfNotPresent(tag, application, null);
	}

	/**
	 * Stores the specified application model with the specified tag if there is no such model.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param application
	 *            The application model.
	 * @param suffix
	 *            A suffix to be appended to the filename.
	 * @return {@code true} if and only if the application model was stored.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveIfNotPresent(String tag, Application application, String suffix) throws IOException {
		Path dirPath = getDir(tag, true, false);

		if (dirPath == null) {
			throw new NotDirectoryException(storagePath.resolve(tag).toAbsolutePath().toString());
		}
		String filename = APPLICATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		Path systemPath = dirPath.resolve(filename);
		boolean exists = new File(systemPath.toString()).exists();

		if (!exists) {
			IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);
			serializer.writeToYaml(application, systemPath);

			LOGGER.debug("Wrote annotation to {}.", dirPath);
		} else {
			LOGGER.debug("Did not write annotation. There was already one at {}.", dirPath);
		}

		return !exists;
	}

	/**
	 * Retrieves the application model from the specified tag if possible.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @return The stored application model or {@code null} if there is no such model.
	 * @throws IOException
	 *             If errors during reading the model occur.
	 */
	public Application readApplication(String tag, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			return null;
		}

		String filename = APPLICATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		Path systemPath = dirPath.resolve(filename);

		if (!systemPath.toFile().exists()) {
			LOGGER.info("There is no file {}.", systemPath.toAbsolutePath());
			return null;
		}

		IdpaYamlSerializer<Application> serializer = new IdpaYamlSerializer<>(Application.class);

		LOGGER.debug("Reading application model from {}.", systemPath);
		return serializer.readFromYaml(systemPath);
	}

	/**
	 * Retrieves the application model from the specified tag if possible.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @return The stored application model or {@code null} if there is no such model.
	 * @throws IOException
	 *             If errors during reading the model occur.
	 */
	public Application readApplication(String tag) throws IOException {
		return readApplication(tag, null);
	}

	/**
	 * Retrieves the annotation from the specified tag if possible.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param suffix
	 *            A suffix to be appended to the file name.
	 * @return The stored annotation or {@code null} if there is no such annotation.
	 * @throws IOException
	 *             If errors during reading the annotation occur.
	 */
	public ApplicationAnnotation readAnnotation(String tag, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			return null;
		}

		IdpaYamlSerializer<ApplicationAnnotation> serializer = new IdpaYamlSerializer<>(ApplicationAnnotation.class);

		String filename = ANNOTATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		Path annotationPath = dirPath.resolve(filename);
		LOGGER.debug("Reading annotation from {}.", dirPath);

		if (!annotationPath.toFile().exists()) {
			LOGGER.info("There is no file {}.", annotationPath.toAbsolutePath());
			return null;
		}

		return serializer.readFromYaml(annotationPath);
	}

	/**
	 * Retrieves the annotation from the specified tag if possible.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return The stored annotation or {@code null} if there is no such annotation.
	 * @throws IOException
	 *             If errors during reading the annotation occur.
	 */
	public ApplicationAnnotation readAnnotation(String tag) throws IOException {
		return readAnnotation(tag, null);
	}

	/**
	 * Removes the specified application model from the storage if possible.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param suffix
	 *            The suffix of the application model.
	 * @return {@code true} if and only if there was a application model and is was successfully
	 *         deleted, {@code false} otherwise.
	 */
	public boolean removeApplicationIfPresent(String tag, String suffix) {
		String filename = APPLICATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		return removeFileIfPresent(tag, filename);
	}

	/**
	 * Removes the specified annotation from the storage if possible.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param suffix
	 *            The suffix of the annotation.
	 * @return {@code true} if and only if there was an annotation and is was successfully deleted,
	 *         {@code false} otherwise.
	 */
	public boolean removeAnnotationIfPresent(String tag, String suffix) {
		String filename = ANNOTATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		return removeFileIfPresent(tag, filename);
	}

	private boolean removeFileIfPresent(String tag, String filename) {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			LOGGER.warn("Could not delete file {} from tag {}.", filename, tag);
			return false;
		}

		Path annotationPath = dirPath.resolve(filename);
		LOGGER.debug("Deleting annotation from {}.", annotationPath);

		return annotationPath.toFile().delete();
	}

	/**
	 * Returns whether there is a application model file for the specified tag and with the
	 * specified suffix.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @param suffix
	 *            A suffix to be appended to the file name.
	 * @return {@code true} if there is a application model with the tag and suffix.
	 */
	public boolean applicationSuffixExists(String tag, String suffix) {
		String filename = APPLICATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		return fileExists(tag, filename);
	}

	/**
	 * Returns whether there is an annotation file for the specified tag and with the specified
	 * suffix.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param suffix
	 *            A suffix to be appended to the file name.
	 * @return {@code true} if there is an annotation with the tag and suffix.
	 */
	public boolean annotationSuffixExists(String tag, String suffix) {
		String filename = ANNOTATION_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		return fileExists(tag, filename);
	}

	private boolean fileExists(String tag, String filename) {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			return false;
		}

		Path annotationPath = dirPath.resolve(filename);
		return annotationPath.toFile().exists();
	}

	/**
	 * Marks the annotation with the passed tag to be broken.
	 *
	 * @param tag
	 * @throws IOException
	 */
	public void markAsBroken(String tag) throws IOException {
		Path dir = getDir(tag, false, true);

		if (dir == null) {
			LOGGER.warn("Could not mark tag {} as broken!", tag);
			return;
		} else {
			Files.write(dir.resolve(BROKEN_FILE_NAME), Collections.singletonList(BROKEN_CONTENT), StandardOpenOption.CREATE);
		}
	}

	/**
	 * Removes a potentially existing mark of the annotation with the passed tag to be broken.
	 *
	 * @param tag
	 * @return {@code true} if there was a mark or {@code false} otherwise.
	 * @throws IOException
	 */
	public boolean unmarkAsBroken(String tag) throws IOException {
		Path dir = getDir(tag, false, true);

		if (dir == null) {
			LOGGER.warn("Could not unmark tag {} as broken!", tag);
			return false;
		} else {
			return Files.deleteIfExists(dir.resolve(BROKEN_FILE_NAME));
		}
	}

	/**
	 * Returns whether the annotation with the passed tag is marked as broken.
	 *
	 * @param tag
	 * @return
	 */
	public boolean isMarkedAsBroken(String tag) {
		Path dir = getDir(tag, false, true);

		if (dir == null) {
			return false;
		} else {
			return Files.exists(dir.resolve(BROKEN_FILE_NAME));
		}
	}

	/**
	 * Returns the legacy application of version lower than 1.0.
	 *
	 * @param tag
	 *            The tag of the application.
	 * @return The legacy application as string.
	 * @throws IOException
	 *             If the application cannot be read.
	 */
	public String readLegacyApplication(String tag) throws IOException {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			return null;
		}

		String filename = LEGACY_APPLICATION_FILE_NAME + FILE_EXTENSION;

		Path systemPath = dirPath.resolve(filename);

		if (!systemPath.toFile().exists()) {
			LOGGER.info("There is no file {}.", systemPath.toAbsolutePath());
			return null;
		}

		String application = reduceLinesToString(Files.readAllLines(systemPath));

		LOGGER.debug("Reading legacy application model from {}.", systemPath);
		return application;
	}

	/**
	 * Returns the legacy annotation of version lower than 1.0.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return The legacy annotation as string.
	 * @throws IOException
	 *             If the annotation cannot be read.
	 */
	public String readLegacyAnnotation(String tag) throws IOException {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			return null;
		}

		String filename = ANNOTATION_FILE_NAME + FILE_EXTENSION;

		Path annotationPath = dirPath.resolve(filename);
		LOGGER.debug("Reading legacy annotation from {}.", dirPath);

		if (!annotationPath.toFile().exists()) {
			LOGGER.info("There is no file {}.", annotationPath.toAbsolutePath());
			return null;
		}

		String annotation = reduceLinesToString(Files.readAllLines(annotationPath));

		return annotation;
	}

	private Path getDir(String tag, boolean createIfAbsent, boolean expectPresence) {
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();

		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.warn("{} is not a directory!", dir.getAbsolutePath());
			return null;
		}

		if (createIfAbsent) {
			dir.mkdirs();
		}

		if (expectPresence && !dir.exists()) {
			LOGGER.warn("{} does not exist!", dir.getAbsolutePath());
			return null;
		}

		return dirPath;
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
