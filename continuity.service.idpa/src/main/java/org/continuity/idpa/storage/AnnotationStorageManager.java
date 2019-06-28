package org.continuity.idpa.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.commons.idpa.AnnotationValidityChecker;
import org.continuity.idpa.AppId;
import org.continuity.idpa.Idpa;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.storage.IdpaStorage.IdpaEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the annotations stored in an {@link IdpaStorage}.
 *
 * @author Henning Schulz
 *
 */
public class AnnotationStorageManager implements IdpaStorageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationStorageManager.class);

	private final IdpaStorage storage;

	public AnnotationStorageManager(IdpaStorage storage) {
		this.storage = storage;
		this.storage.registerListener(this);
	}

	/**
	 * Retrieves the specified annotation if present.
	 *
	 * @param aid
	 *            The app-id of the annotation.
	 * @return A {@link ApplicationAnnotation}. If there is no annotation for the app-id,
	 *         {@code null} will be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation read(AppId aid) throws IOException {
		return storage.readLatest(aid).getAnnotation();
	}

	/**
	 * Retrieves the specified annotation for a given version or timestamp if present.
	 *
	 * @param aid
	 *            The app-id of the annotation.
	 * @param version
	 *            The version or timestamp for which an application model is searched.
	 * @return A {@link ApplicationAnnotation}. If there is no annotation for the app-id,
	 *         {@code null} will be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation read(AppId aid, VersionOrTimestamp version) throws IOException {
		return storage.readLatestBefore(aid, version).getAnnotation();
	}

	/**
	 * Updates the annotation stored with the specified app-id. If the annotation is invalid with
	 * respect to the application model, it is rejected. It does <b>not</b> try to fix it. If you
	 * want to store an annotation that covers application parts that are not part of the current
	 * application model, please update the application model first.<br>
	 * Assumes a version or timestamp and a corresponding application model to be present.
	 *
	 * @param aid
	 * @param annotation
	 * @return A report holding information about the changes and if the new annotation is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport saveOrUpdate(AppId aid, ApplicationAnnotation annotation) throws IOException {
		if (annotation.getVersionOrTimestamp().isEmpty()) {
			throw new IllegalArgumentException("Cannot store an annotation without a version or timestamp! Either needs to be set or passed as assitional argument.");
		}

		Idpa latest = storage.readLatestBefore(aid, annotation.getVersionOrTimestamp());

		if (latest == null) {
			throw new IllegalStateException("There is no application model with app-id " + aid);
		}

		AnnotationValidityChecker checker = new AnnotationValidityChecker(latest.getApplication());
		checker.checkAnnotation(annotation);
		AnnotationValidityReport report = checker.getReport();

		if (!report.isBreaking()) {
			storage.save(aid, annotation);
			LOGGER.info("Stored an annotation with app-id {} and version {}.", aid, annotation.getVersionOrTimestamp());

			storage.unmarkAsBroken(aid, annotation.getVersionOrTimestamp());
		}

		return report;
	}

	/**
	 * Updates the annotation stored with the specified app-id. If the annotation is invalid with
	 * respect to the application model, it is rejected. It does <b>not</b> try to fix it. If you
	 * want to store an annotation that covers application parts that are not part of the current
	 * application model, please update the application model first.<br>
	 * Assumes a corresponding application model to be present.
	 *
	 * @param aid
	 * @param version
	 * @param annotation
	 * @return A report holding information about the changes and if the new annotation is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport saveOrUpdate(AppId aid, VersionOrTimestamp version, ApplicationAnnotation annotation) throws IOException {
		annotation.setVersionOrTimestamp(version);
		return saveOrUpdate(aid, annotation);
	}

	/**
	 * Returns whether the latest annotation with the specified app-id is broken.
	 *
	 * @param aid
	 * @return {@code true} if it is broken or {@code false} otherwise.
	 */
	public boolean isBroken(AppId aid) {
		return storage.readLatest(aid).checkAdditionalFlag(IdpaStorage.FLAG_BROKEN);
	}

	/**
	 * Returns whether the annotation for the specified app-id and version or timestamp is broken.
	 *
	 * @param aid
	 * @param version
	 * @return {@code true} if it is broken or {@code false} otherwise.
	 */
	public boolean isBroken(AppId aid, VersionOrTimestamp version) {
		return storage.isBroken(aid, version);
	}

	/**
	 * Determines all annotations that belong to a certain application model and are broken.
	 *
	 * @param aid
	 *            The app-id.
	 * @param version
	 *            The version or timestamp of the application model.
	 * @return A list of all versions or timestamps of annotations that are broken.
	 */
	public List<VersionOrTimestamp> getBrokenForApplication(AppId aid, VersionOrTimestamp version) {
		List<VersionOrTimestamp> broken = new ArrayList<>();

		applyForApplication(aid, version, (t, idpa) -> {
			if (isBroken(aid, idpa.getVersionOrTimestamp())) {
				broken.add(idpa.getVersionOrTimestamp());
			}
		});

		return broken;
	}

	/**
	 * {@inheritDoc} <br>
	 *
	 * Checks whether the annotations affected by the change are broken and marks them accordingly.
	 */
	@Override
	public void onApplicationChanged(AppId aid, VersionOrTimestamp version) {
		applyForApplication(aid, version, this::adjustBrokenMark);
	}

	private void applyForApplication(AppId aid, VersionOrTimestamp version, BiConsumer<AppId, Idpa> consumer) {
		Iterator<IdpaEntry> it = storage.iterate(aid).iterator();

		IdpaEntry curr = null;

		while (it.hasNext() && ((curr == null) || curr.getApplication().getVersionOrTimestamp().after(version))) {
			curr = it.next();
		}

		if ((curr != null)) {
			consumer.accept(aid, curr);
		}

		while (it.hasNext() && (curr != null) && curr.getApplication().getVersionOrTimestamp().equals(version)) {
			curr = it.next();
			consumer.accept(aid, curr);
		}
	}

	private boolean isBroken(Idpa idpa) {
		if (idpa.getAnnotation() == null) {
			return false;
		}

		AnnotationValidityChecker checker = new AnnotationValidityChecker(idpa.getApplication());
		checker.checkAnnotation(idpa.getAnnotation());
		AnnotationValidityReport report = checker.getReport();

		return report.isBreaking();
	}

	private void adjustBrokenMark(AppId aid, Idpa idpa) {
		if (isBroken(idpa)) {
			try {
				storage.markAsBroken(aid, idpa.getVersionOrTimestamp());
				LOGGER.warn("Annotation of app-id {} for version {} is now broken.", aid, idpa.getVersionOrTimestamp());
			} catch (IOException e) {
				LOGGER.error("Could not mark annotation " + aid + " (" + idpa.getVersionOrTimestamp() + ") as broken!", e);
			}
		} else {
			try {
				storage.unmarkAsBroken(aid, idpa.getVersionOrTimestamp());
				LOGGER.info("Annotation of app-id {} for version {} is not broken (anymore).", aid, idpa.getVersionOrTimestamp());
			} catch (IOException e) {
				LOGGER.error("Could not unmark annotation " + aid + " (" + idpa.getVersionOrTimestamp() + ") as broken!", e);
			}
		}
	}

	@Override
	public void onAnnotationChanged(AppId aid, VersionOrTimestamp version) {
		// do nothing
	}

}
