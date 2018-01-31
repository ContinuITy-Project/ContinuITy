package org.continuity.system.annotation.storage;

import java.io.IOException;

import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.system.annotation.entities.AnnotationValidityReport;
import org.continuity.system.annotation.validation.AnnotationFixer;
import org.continuity.system.annotation.validation.AnnotationValidityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationStorageManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationStorageManager.class);

	private static final String SUFFIX_BASE = "base";

	private final AnnotationStorage storage;

	@Autowired
	public AnnotationStorageManager(AnnotationStorage storage) {
		this.storage = storage;
	}

	/**
	 * Retrieves the specified system model if present.
	 *
	 * @param tag
	 *            The tag of the system model.
	 * @return A {@link SystemModel}. If there is no system model for the tag, {@code null} will be
	 *         returned.
	 * @throws IOException
	 */
	public SystemModel getSystemModel(String tag) throws IOException {
		return storage.readSystemModel(tag);
	}

	/**
	 * Retrieves the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return A {@link SystemAnnotation}. If there is no annotation for the tag, {@code null} will
	 *         be returned.
	 * @throws IOException
	 */
	public SystemAnnotation getAnnotation(String tag) throws IOException {
		return storage.readAnnotation(tag);
	}

	/**
	 * Retrieves the base of the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return A {@link SystemAnnotation}. If there is no base annotation for the tag, {@code null}
	 *         will be returned.
	 * @throws IOException
	 */
	public SystemAnnotation getBaseAnnotation(String tag) throws IOException {
		return storage.readAnnotation(tag, SUFFIX_BASE);
	}

	/**
	 * Updates the system model stored with the specified tag. If the system model breaks the stored
	 * annotation, the annotation is tried to be fixed if possible.
	 *
	 * @param tag
	 * @param annotation
	 * @param systemChangeReport
	 *            Report holding the system changes.
	 * @return A report holding information about the changes and if the current state is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport updateSystemModel(String tag, SystemModel system, AnnotationValidityReport systemChangeReport) throws IOException {
		if ((systemChangeReport.getSystemChanges() == null) || systemChangeReport.getSystemChanges().isEmpty()) {
			return AnnotationValidityReport.empty();
		}

		storage.unmarkAsBroken(tag);

		SystemModel oldSystemModel = storage.readSystemModel(tag);
		SystemAnnotation annotation = storage.readAnnotation(tag);

		storage.saveOrUpdate(tag, system);

		AnnotationValidityReport report = checkEverything(system, annotation, systemChangeReport);

		if (report.isBreaking()) {
			AnnotationFixer fixer = new AnnotationFixer();
			SystemAnnotation fixedAnnotation = fixer.createFixedAnnotation(annotation, report);

			AnnotationValidityReport newReport = checkEverything(system, fixedAnnotation, systemChangeReport);
			newReport.setViolationsBeforeFix(report.getViolations());
			report = newReport;

			if (!newReport.isBreaking()) {
				storage.saveOrUpdate(tag, fixedAnnotation);
				LOGGER.info("Fixed annotation for tag {}.", tag);
			} else {
				storage.removeAnnotationIfPresent(tag, null);
				storage.markAsBroken(tag);
				LOGGER.warn("The annotation for tag {} is now in a broken state!", tag);
			}

			storage.saveIfNotPresent(tag, annotation, SUFFIX_BASE);
			storage.saveIfNotPresent(tag, oldSystemModel, SUFFIX_BASE);
			LOGGER.info("Created or updated base models for tag {}.", tag);
		}

		return report;
	}

	private AnnotationValidityReport checkEverything(SystemModel newSystemModel, SystemAnnotation annotation, AnnotationValidityReport systemChangeReport) {
		AnnotationValidityChecker checker = new AnnotationValidityChecker(newSystemModel);
		checker.registerSystemChanges(systemChangeReport);

		if (annotation != null) {
			checker.checkAnnotation(annotation);
		}

		return checker.getReport();
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the system model, it is rejected. It does <b>not</b> try to fix it. If you want to
	 * store an annotation that covers system parts that are not part of the current system model,
	 * please update the system model first.<br>
	 * Assumes the corresponding system model to be present.
	 *
	 * @param tag
	 * @param annotation
	 * @return A report holding information about the changes and if the new annotation is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport updateAnnotation(String tag, SystemAnnotation annotation) throws IOException {
		SystemModel systemModel = storage.readSystemModel(tag);

		if (systemModel == null) {
			throw new IllegalStateException("There is no system model with tag " + tag);
		}

		AnnotationValidityChecker checker = new AnnotationValidityChecker(systemModel);
		checker.checkAnnotation(annotation);
		AnnotationValidityReport report = checker.getReport();

		if (!report.isBreaking()) {
			storage.saveOrUpdate(tag, annotation);

			storage.unmarkAsBroken(tag);
			deleteBaseAndLog(tag);
		}

		return report;
	}

	/**
	 * Stores the annotation if there is not yet one for the specified tag.
	 *
	 * @param tag
	 * @param annotation
	 * @return true if and only if the annotation was stored.
	 * @throws IOException
	 */
	public boolean saveAnnotationIfNotPresent(String tag, SystemAnnotation annotation) throws IOException {
		return storage.saveIfNotPresent(tag, annotation);
	}

	/**
	 * Creates or updates a system model and an annotation with the specified tag and creates a
	 * validity report. Existing annotations are not overwritten.
	 *
	 * @param tag
	 * @param system
	 * @param annotation
	 * @param systemChangeReport
	 *            Report holding the system changes.
	 * @return
	 * @throws IOException
	 */
	public AnnotationValidityReport createOrUpdate(String tag, SystemModel system, SystemAnnotation annotation, AnnotationValidityReport systemChangeReport) throws IOException {
		storage.saveIfNotPresent(tag, annotation);
		return updateSystemModel(tag, system, systemChangeReport);
	}

	/**
	 * Returns whether the annotation with the specified tag is broken.
	 *
	 * @param tag
	 * @return {@code true} if it is broken or {@code false} otherwise.
	 */
	public boolean isBroken(String tag) {
		return storage.isMarkedAsBroken(tag);
	}

	private void deleteBaseAndLog(String tag) {
		boolean deleted = storage.removeAnnotationIfPresent(tag, SUFFIX_BASE);
		deleted &= storage.removeSystemIfPresent(tag, SUFFIX_BASE);

		if (deleted) {
			LOGGER.info("Deleted base system model and annotation with tag {}.", tag);
		} else {
			LOGGER.debug("Did not delete base system model and annotation with tag {}. Potentially, there was no base.", tag);
		}
	}

}
