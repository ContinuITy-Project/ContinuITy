package org.continuity.system.annotation.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.yaml.ContinuityYamlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationStorage.class);

	private static final String SYSTEM_MODEL_FILE_NAME = "system";
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
	 * @param systemModel
	 *            The system model.
	 * @param annotation
	 *            The annotation
	 * @param suffix
	 *            A suffix to be appended to the file name of the annotation.
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, SystemModel systemModel, SystemAnnotation annotation, String suffix) throws IOException {
		boolean created = saveOrUpdate(tag, systemModel);
		created = created || saveOrUpdate(tag, annotation, suffix);

		return !created;
	}

	/**
	 * Stores the specified models with the specified tag. If there are already models with the same
	 * tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the models.
	 * @param systemModel
	 *            The system model.
	 * @param annotation
	 *            The annotation
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, SystemModel systemModel, SystemAnnotation annotation) throws IOException {
		return saveOrUpdate(tag, systemModel, annotation, null);
	}

	/**
	 * Stores the specified system model with the specified tag. If there are already models with
	 * the same tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param systemModel
	 *            The system model.
	 * @param suffix
	 *            A suffix to be appended to the file name.
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, SystemModel systemModel, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, false);

		if (dirPath == null) {
			throw new NotDirectoryException(storagePath.resolve(tag).toAbsolutePath().toString());
		}

		boolean created = dirPath.toFile().mkdirs();

		String filename = SYSTEM_MODEL_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);
		serializer.writeToYaml(systemModel, dirPath.resolve(filename));

		LOGGER.debug("Wrote system model to {}.", dirPath);

		return !created;
	}

	/**
	 * Stores the specified system model with the specified tag. If there are already models with
	 * the same tag, they will be overwritten.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param systemModel
	 *            The system model.
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, SystemModel systemModel) throws IOException {
		return saveOrUpdate(tag, systemModel, (String) null);
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
	public boolean saveOrUpdate(String tag, SystemAnnotation annotation, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, false);

		if (dirPath == null) {
			throw new NotDirectoryException(storagePath.resolve(tag).toAbsolutePath().toString());
		}

		boolean created = dirPath.toFile().mkdirs();

		ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);

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
	public boolean saveOrUpdate(String tag, SystemAnnotation annotation) throws IOException {
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
	public boolean saveIfNotPresent(String tag, SystemAnnotation annotation) throws IOException {
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
	public boolean saveIfNotPresent(String tag, SystemAnnotation annotation, String suffix) throws IOException {
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
			ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);
			serializer.writeToYaml(annotation, annotationPath);

			LOGGER.debug("Wrote annotation to {}.", dirPath);
		} else {
			LOGGER.debug("Did not write annotation. There was already one at {}.", dirPath);
		}

		return !exists;
	}

	/**
	 * Stores the specified system model with the specified tag if there is no such model.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param system
	 *            The system model.
	 * @return {@code true} if and only if the system model was stored.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveIfNotPresent(String tag, SystemModel system) throws IOException {
		return saveIfNotPresent(tag, system, null);
	}

	/**
	 * Stores the specified system model with the specified tag if there is no such model.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param system
	 *            The system model.
	 * @param suffix
	 *            A suffix to be appended to the filename.
	 * @return {@code true} if and only if the system model was stored.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveIfNotPresent(String tag, SystemModel system, String suffix) throws IOException {
		Path dirPath = getDir(tag, true, false);

		if (dirPath == null) {
			throw new NotDirectoryException(storagePath.resolve(tag).toAbsolutePath().toString());
		}
		String filename = SYSTEM_MODEL_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		Path systemPath = dirPath.resolve(filename);
		boolean exists = new File(systemPath.toString()).exists();

		if (!exists) {
			ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);
			serializer.writeToYaml(system, systemPath);

			LOGGER.debug("Wrote annotation to {}.", dirPath);
		} else {
			LOGGER.debug("Did not write annotation. There was already one at {}.", dirPath);
		}

		return !exists;
	}

	/**
	 * Retrieves the system model from the specified tag if possible.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @return The stored system model or {@code null} if there is no such model.
	 * @throws IOException
	 *             If errors during reading the model occur.
	 */
	public SystemModel readSystemModel(String tag, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			return null;
		}

		String filename = SYSTEM_MODEL_FILE_NAME;

		if (suffix != null) {
			filename += "-" + suffix;
		}

		filename += FILE_EXTENSION;

		Path systemPath = dirPath.resolve(filename);

		if (!systemPath.toFile().exists()) {
			LOGGER.info("There is no file {}.", systemPath.toAbsolutePath());
			return null;
		}

		ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);

		LOGGER.debug("Reading system model from {}.", systemPath);
		return serializer.readFromYaml(systemPath);
	}

	/**
	 * Retrieves the system model from the specified tag if possible.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @return The stored system model or {@code null} if there is no such model.
	 * @throws IOException
	 *             If errors during reading the model occur.
	 */
	public SystemModel readSystemModel(String tag) throws IOException {
		return readSystemModel(tag, null);
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
	public SystemAnnotation readAnnotation(String tag, String suffix) throws IOException {
		Path dirPath = getDir(tag, false, true);

		if (dirPath == null) {
			return null;
		}

		ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);

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
	public SystemAnnotation readAnnotation(String tag) throws IOException {
		return readAnnotation(tag, null);
	}

	/**
	 * Removes the specified system model from the storage if possible.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param suffix
	 *            The suffix of the system model.
	 * @return {@code true} if and only if there was a system model and is was successfully deleted,
	 *         {@code false} otherwise.
	 */
	public boolean removeSystemIfPresent(String tag, String suffix) {
		String filename = SYSTEM_MODEL_FILE_NAME;

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
	 * Returns whether there is a system model file for the specified tag and with the specified
	 * suffix.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @param suffix
	 *            A suffix to be appended to the file name.
	 * @return {@code true} if there is a system model with the tag and suffix.
	 */
	public boolean systemSuffixExists(String tag, String suffix) {
		String filename = SYSTEM_MODEL_FILE_NAME;

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

}
