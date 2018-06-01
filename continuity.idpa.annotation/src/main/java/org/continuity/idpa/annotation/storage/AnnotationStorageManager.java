package org.continuity.idpa.annotation.storage;

import java.io.IOException;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.validation.AnnotationFixer;
import org.continuity.idpa.annotation.validation.AnnotationValidityChecker;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.legacy.IdpaFromOldAnnotationConverter;
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
	 * Retrieves the specified application model if present.
	 *
	 * @param tag
	 *            The tag of the application model.
	 * @return A {@link Application}. If there is no application model for the tag, {@code null} will be
	 *         returned.
	 * @throws IOException
	 */
	public Application getApplication(String tag) throws IOException {
		return storage.readApplication(tag);
	}

	/**
	 * Retrieves the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return A {@link ApplicationAnnotation}. If there is no annotation for the tag, {@code null} will
	 *         be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation getAnnotation(String tag) throws IOException {
		return storage.readAnnotation(tag);
	}

	/**
	 * Retrieves the base of the specified annotation if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @return A {@link ApplicationAnnotation}. If there is no base annotation for the tag, {@code null}
	 *         will be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation getBaseAnnotation(String tag) throws IOException {
		return storage.readAnnotation(tag, SUFFIX_BASE);
	}

	/**
	 * Updates the application model stored with the specified tag. If the application model breaks
	 * the stored annotation, the annotation is tried to be fixed if possible.
	 *
	 * @param tag
	 * @param annotation
	 * @param applicationChangeReport
	 *            Report holding the application changes.
	 * @return A report holding information about the changes and if the current state is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport updateApplication(String tag, Application application, AnnotationValidityReport applicationChangeReport) throws IOException {
		if ((applicationChangeReport.getApplicationChanges() == null) || applicationChangeReport.getApplicationChanges().isEmpty()) {
			return AnnotationValidityReport.empty();
		}

		storage.unmarkAsBroken(tag);

		Application oldSystemModel = storage.readApplication(tag);
		ApplicationAnnotation annotation = storage.readAnnotation(tag);

		storage.saveOrUpdate(tag, application);

		AnnotationValidityReport report = checkEverything(application, annotation, applicationChangeReport);

		if (report.isBreaking()) {
			AnnotationFixer fixer = new AnnotationFixer();
			ApplicationAnnotation fixedAnnotation = fixer.createFixedAnnotation(annotation, report);

			AnnotationValidityReport newReport = checkEverything(application, fixedAnnotation, applicationChangeReport);
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

	private AnnotationValidityReport checkEverything(Application newApplicationModel, ApplicationAnnotation annotation, AnnotationValidityReport applicationChangeReport) {
		AnnotationValidityChecker checker = new AnnotationValidityChecker(newApplicationModel);
		checker.registerApplicationChanges(applicationChangeReport);

		if (annotation != null) {
			checker.checkAnnotation(annotation);
		}

		return checker.getReport();
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the application model, it is rejected. It does <b>not</b> try to fix it. If you
	 * want to store an annotation that covers application parts that are not part of the current
	 * application model, please update the application model first.<br>
	 * Assumes the corresponding application model to be present.
	 *
	 * @param tag
	 * @param annotation
	 * @return A report holding information about the changes and if the new annotation is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport updateAnnotation(String tag, ApplicationAnnotation annotation) throws IOException {
		Application systemModel = storage.readApplication(tag);

		if (systemModel == null) {
			throw new IllegalStateException("There is no application model with tag " + tag);
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
	public boolean saveAnnotationIfNotPresent(String tag, ApplicationAnnotation annotation) throws IOException {
		return storage.saveIfNotPresent(tag, annotation);
	}

	/**
	 * Creates or updates a application model and an annotation with the specified tag and creates a
	 * validity report. Existing annotations are not overwritten.
	 *
	 * @param tag
	 * @param application
	 * @param annotation
	 * @param applicationChangeReport
	 *            Report holding the application changes.
	 * @return
	 * @throws IOException
	 */
	public AnnotationValidityReport createOrUpdate(String tag, Application application, ApplicationAnnotation annotation, AnnotationValidityReport applicationChangeReport) throws IOException {
		storage.saveIfNotPresent(tag, annotation);
		return updateApplication(tag, application, applicationChangeReport);
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

	/**
	 * Updates the legacy application and annotation for versions lower than 1.0.
	 *
	 * @param tag
	 *            The tag of the IDPA.
	 * @return Whether the application or the annotation was updated.
	 * @throws IOException
	 *             If errors during reading the IDPA occur.
	 */
	public boolean updateLegacyIdpa(String tag) throws IOException {
		String legacyApplication = storage.readLegacyApplication(tag);
		String legacyAnnotation = storage.readLegacyAnnotation(tag);

		IdpaFromOldAnnotationConverter converter = new IdpaFromOldAnnotationConverter();
		boolean updated = false;

		if (legacyApplication == null) {
			LOGGER.info("There is no legacy application for tag {} to be updated.", tag);
		} else {
			Application application = converter.convertFromSystemModel(legacyApplication);
			storage.saveOrUpdate(tag, application);
			updated = true;
			LOGGER.info("Updated the legacy application for tag {}.", tag);
		}

		if (legacyAnnotation == null) {
			LOGGER.info("There is no legacy annotation for tag {} to be updated.", tag);
		} else {
			ApplicationAnnotation annotation = converter.convertFromAnnotation(legacyAnnotation);
			storage.saveOrUpdate(tag, annotation);
			updated = true;
			LOGGER.info("Updated the legacy annotation for tag {}.", tag);
		}

		return updated;
	}

	private void deleteBaseAndLog(String tag) {
		boolean deleted = storage.removeAnnotationIfPresent(tag, SUFFIX_BASE);
		deleted &= storage.removeApplicationIfPresent(tag, SUFFIX_BASE);

		if (deleted) {
			LOGGER.info("Deleted base application model and annotation with tag {}.", tag);
		} else {
			LOGGER.debug("Did not delete base application model and annotation with tag {}. Potentially, there was no base.", tag);
		}
	}

}
