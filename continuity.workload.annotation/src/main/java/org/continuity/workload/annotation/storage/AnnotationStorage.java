package org.continuity.workload.annotation.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.continuity.annotation.dsl.ContinuityModelElement;
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

	private static final String SYSTEM_MODEL_FILE_NAME = "system.yml";
	private static final String ANNOTATION_FILE_NAME = "annotation.yml";

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
	 * @return {@code true} if and only if existing models were overwritten.
	 * @throws IOException
	 *             If errors during writing to files occur.
	 */
	public boolean saveOrUpdate(String tag, SystemModel systemModel, SystemAnnotation annotation) throws IOException {
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();


		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.error("{} is not a directory!", dir.getAbsolutePath());
			throw new NotDirectoryException(dir.getAbsolutePath());
		}

		boolean created = dir.mkdirs();
		ContinuityYamlSerializer<ContinuityModelElement> serializer = new ContinuityYamlSerializer<>(ContinuityModelElement.class);

		serializer.writeToYaml(systemModel, dirPath.resolve(SYSTEM_MODEL_FILE_NAME));
		serializer.writeToYaml(annotation, dirPath.resolve(ANNOTATION_FILE_NAME));

		LOGGER.debug("Wrote system model and annotation to {}.", dirPath);

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
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();

		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.error("{} is not a directory!", dir.getAbsolutePath());
			throw new NotDirectoryException(dir.getAbsolutePath());
		}

		boolean created = dir.mkdirs();
		ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);
		serializer.writeToYaml(systemModel, dirPath.resolve(SYSTEM_MODEL_FILE_NAME));

		LOGGER.debug("Wrote system model to {}.", dirPath);

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
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();

		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.error("{} is not a directory!", dir.getAbsolutePath());
			throw new NotDirectoryException(dir.getAbsolutePath());
		}

		boolean created = dir.mkdirs();
		ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);
		serializer.writeToYaml(annotation, dirPath.resolve(ANNOTATION_FILE_NAME));

		LOGGER.debug("Wrote annotation to {}.", dirPath);

		return !created;
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
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();

		if (dir.exists() && !dir.isDirectory()) {
			LOGGER.error("{} is not a directory!", dir.getAbsolutePath());
			throw new NotDirectoryException(dir.getAbsolutePath());
		}

		dir.mkdirs();
		boolean exists = new File(dirPath.resolve(ANNOTATION_FILE_NAME).toString()).exists();

		if (!exists) {
			ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);
			serializer.writeToYaml(annotation, dirPath.resolve(ANNOTATION_FILE_NAME));

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
	public SystemModel readSystemModel(String tag) throws IOException {
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();

		if (!dir.exists() || !dir.isDirectory()) {
			LOGGER.warn("There is no directory {}!", dir.getAbsolutePath());
			return null;
		}

		ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);

		LOGGER.debug("Reading system model from {}.", dirPath);
		return serializer.readFromYaml(dirPath.resolve(SYSTEM_MODEL_FILE_NAME));
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
		Path dirPath = storagePath.resolve(tag);
		File dir = dirPath.toFile();

		if (!dir.exists() || !dir.isDirectory()) {
			LOGGER.warn("There is no directory {}!", dir.getAbsolutePath());
			return null;
		}

		ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);

		LOGGER.debug("Reading annotation from {}.", dirPath);
		return serializer.readFromYaml(dirPath.resolve(ANNOTATION_FILE_NAME));
	}

}
