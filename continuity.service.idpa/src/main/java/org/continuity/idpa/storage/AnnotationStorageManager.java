package org.continuity.idpa.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.commons.idpa.AnnotationValidityChecker;
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
	 * @param tag
	 *            The tag of the annotation.
	 * @return A {@link ApplicationAnnotation}. If there is no annotation for the tag, {@code null} will
	 *         be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation read(String tag) throws IOException {
		return storage.readLatest(tag).getAnnotation();
	}

	/**
	 * Retrieves the specified annotation for a given version or timestamp if present.
	 *
	 * @param tag
	 *            The tag of the annotation.
	 * @param version
	 *            The version or timestamp for which an application model is searched.
	 * @return A {@link ApplicationAnnotation}. If there is no annotation for the tag, {@code null}
	 *         will be returned.
	 * @throws IOException
	 */
	public ApplicationAnnotation read(String tag, VersionOrTimestamp version) throws IOException {
		return storage.readLatestBefore(tag, version).getAnnotation();
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the application model, it is rejected. It does <b>not</b> try to fix it. If you
	 * want to store an annotation that covers application parts that are not part of the current
	 * application model, please update the application model first.<br>
	 * Assumes a version or timestamp and a corresponding application model to be present.
	 *
	 * @param tag
	 * @param annotation
	 * @return A report holding information about the changes and if the new annotation is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport saveOrUpdate(String tag, ApplicationAnnotation annotation) throws IOException {
		if (annotation.getVersionOrTimestamp().isEmpty()) {
			throw new IllegalArgumentException("Cannot store an annotation without a version or timestamp! Either needs to be set or passed as assitional argument.");
		}

		Idpa latest = storage.readLatestBefore(tag, annotation.getVersionOrTimestamp());

		if (latest == null) {
			throw new IllegalStateException("There is no application model with tag " + tag);
		}

		AnnotationValidityChecker checker = new AnnotationValidityChecker(latest.getApplication());
		checker.checkAnnotation(annotation);
		AnnotationValidityReport report = checker.getReport();

		if (!report.isBreaking()) {
			storage.save(tag, annotation);
			LOGGER.info("Stored an annotation with tag {} and version {}.", tag, annotation.getVersionOrTimestamp());

			storage.unmarkAsBroken(tag, annotation.getVersionOrTimestamp());
		}

		return report;
	}

	/**
	 * Updates the annotation stored with the specified tag. If the annotation is invalid with
	 * respect to the application model, it is rejected. It does <b>not</b> try to fix it. If you
	 * want to store an annotation that covers application parts that are not part of the current
	 * application model, please update the application model first.<br>
	 * Assumes a corresponding application model to be present.
	 *
	 * @param tag
	 * @param version
	 * @param annotation
	 * @return A report holding information about the changes and if the new annotation is broken.
	 * @throws IOException
	 */
	public AnnotationValidityReport saveOrUpdate(String tag, VersionOrTimestamp version, ApplicationAnnotation annotation) throws IOException {
		annotation.setVersionOrTimestamp(version);
		return saveOrUpdate(tag, annotation);
	}

	/**
	 * Returns whether the latest annotation with the specified tag is broken.
	 *
	 * @param tag
	 * @return {@code true} if it is broken or {@code false} otherwise.
	 */
	public boolean isBroken(String tag) {
		return storage.readLatest(tag).checkAdditionalFlag(IdpaStorage.FLAG_BROKEN);
	}

	/**
	 * Returns whether the annotation for the specified tag and version or timestamp is broken.
	 *
	 * @param tag
	 * @param version
	 * @return {@code true} if it is broken or {@code false} otherwise.
	 */
	public boolean isBroken(String tag, VersionOrTimestamp version) {
		return storage.isBroken(tag, version);
	}

	/**
	 * Determines all annotations that belong to a certain application model and are broken.
	 *
	 * @param tag
	 *            The tag.
	 * @param version
	 *            The version or timestamp of the application model.
	 * @return A list of all versions or timestamps of annotations that are broken.
	 */
	public List<VersionOrTimestamp> getBrokenForApplication(String tag, VersionOrTimestamp version) {
		List<VersionOrTimestamp> broken = new ArrayList<>();

		applyForApplication(tag, version, (t, idpa) -> {
			if (isBroken(tag, idpa.getVersionOrTimestamp())) {
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
	public void onApplicationChanged(String tag, VersionOrTimestamp version) {
		applyForApplication(tag, version, this::adjustBrokenMark);
	}

	private void applyForApplication(String tag, VersionOrTimestamp version, BiConsumer<String, Idpa> consumer) {
		Iterator<IdpaEntry> it = storage.iterate(tag).iterator();

		IdpaEntry curr = null;

		while (it.hasNext() && ((curr == null) || curr.getApplication().getVersionOrTimestamp().after(version))) {
			curr = it.next();
		}

		if ((curr != null)) {
			consumer.accept(tag, curr);
		}

		while (it.hasNext() && (curr != null) && curr.getApplication().getVersionOrTimestamp().equals(version)) {
			curr = it.next();
			consumer.accept(tag, curr);
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

	private void adjustBrokenMark(String tag, Idpa idpa) {
		if (isBroken(idpa)) {
			try {
				storage.markAsBroken(tag, idpa.getVersionOrTimestamp());
				LOGGER.warn("Annotation of tag {} for version {} is now broken.", tag, idpa.getVersionOrTimestamp());
			} catch (IOException e) {
				LOGGER.error("Could not mark annotation " + tag + " (" + idpa.getVersionOrTimestamp() + ") as broken!", e);
			}
		} else {
			try {
				storage.unmarkAsBroken(tag, idpa.getVersionOrTimestamp());
				LOGGER.info("Annotation of tag {} for version {} is not broken (anymore).", tag, idpa.getVersionOrTimestamp());
			} catch (IOException e) {
				LOGGER.error("Could not unmark annotation " + tag + " (" + idpa.getVersionOrTimestamp() + ") as broken!", e);
			}
		}
	}

	@Override
	public void onAnnotationChanged(String tag, VersionOrTimestamp version) {
		// do nothing
	}

}
