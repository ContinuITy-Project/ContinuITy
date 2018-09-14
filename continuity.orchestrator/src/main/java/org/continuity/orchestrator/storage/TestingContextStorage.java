package org.continuity.orchestrator.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.commons.storage.ArtifactStorage;
import org.continuity.commons.storage.JsonFileStorage;
import org.continuity.commons.storage.TagFileStorage;
import org.continuity.orchestrator.entities.TestingContextMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestingContextStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestingContextStorage.class);

	private static final String ARTIFACTS_DIR = "artifacts";

	private static final String MAPPINGS_DIR = "mappings";

	private final ArtifactStorage<LinkExchangeModel> artifactStorage;

	private final TagFileStorage<TestingContextMapping> mappingStorage;

	public TestingContextStorage(Path storagePath) {
		this.artifactStorage = new JsonFileStorage<LinkExchangeModel>(storagePath.resolve(ARTIFACTS_DIR), new LinkExchangeModel());
		this.mappingStorage = new TagFileStorage<>(storagePath.resolve(MAPPINGS_DIR), TestingContextMapping.class);
	}

	public void store(String tag, Set<String> testingContext, LinkExchangeModel artifact) throws IOException {
		Pair<String, LinkExchangeModel> existing = mergeWithExisting(tag, testingContext, artifact);
		String id;

		if (existing == null) {
			LOGGER.info("Creating a new source entity for testing-context {}", testingContext);
			id = artifactStorage.put(artifact, tag);
		} else {
			LOGGER.info("Updating an existing source entity for testing-context {}", testingContext);
			id = existing.getKey();
			artifact.merge(existing.getValue());
			artifactStorage.putToReserved(id, artifact);
		}

		TestingContextMapping mapping = mappingStorage.read(tag);

		if (mapping == null) {
			mapping = new TestingContextMapping();
		}

		mapping.addMapping(testingContext, id);

		mappingStorage.store(mapping, tag);
	}

	private Pair<String, LinkExchangeModel> mergeWithExisting(String tag, Set<String> testingContext, LinkExchangeModel artifact) throws IOException {
		Map<String, LinkExchangeModel> existing = getFull(tag, testingContext);

		for (Map.Entry<String, LinkExchangeModel> entry : existing.entrySet()) {
			if (overlap(entry.getValue(), artifact)) {
				return Pair.of(entry.getKey(), entry.getValue());
			}
		}

		return null;
	}

	public Map<Set<String>, Set<LinkExchangeModel>> get(String tag, Set<String> testingContext, boolean includePartial) throws IOException {
		TestingContextMapping mapping = mappingStorage.read(tag);

		if (mapping == null) {
			return Collections.emptyMap();
		}

		Map<Set<String>, Set<LinkExchangeModel>> result = new HashMap<>();

		if (includePartial) {
			for (String ctx : testingContext) {
				Set<Set<String>> indivMapping = mapping.getIndividualMappings().get(ctx);

				if (indivMapping != null) {
					for (Set<String> partialContext : indivMapping) {
						Set<LinkExchangeModel> sources = new HashSet<>();

						for (String id : mapping.getFullMappings().get(partialContext)) {
							sources.add(artifactStorage.get(id));
						}

						if (!sources.isEmpty()) {
							result.put(partialContext, sources);
						}
					}
				}
			}
		} else {
			Set<LinkExchangeModel> sources = new HashSet<>();
			sources.addAll(getFull(tag, testingContext).values());

			if (!sources.isEmpty()) {
				result.put(testingContext, sources);
			}
		}

		return result;
	}

	private Map<String, LinkExchangeModel> getFull(String tag, Set<String> testingContext) throws IOException {
		TestingContextMapping mapping = mappingStorage.read(tag);
		Set<String> fullMapping = mapping.getFullMappings().get(testingContext);

		Map<String, LinkExchangeModel> sources = new HashMap<>();

		if (fullMapping != null) {
			for (String id : fullMapping) {
				sources.put(id, artifactStorage.get(id));
			}
		}

		return sources;
	}

	private boolean overlap(LinkExchangeModel first, LinkExchangeModel second) {
		boolean overlap = false;

		overlap |= (first.getMeasurementDataLinks().getLink() != null) && Objects.equals(first.getMeasurementDataLinks().getLink(), second.getMeasurementDataLinks().getLink());
		overlap |= (first.getSessionLogsLinks().getLink() != null) && Objects.equals(first.getSessionLogsLinks().getLink(), second.getSessionLogsLinks().getLink());
		overlap |= (first.getWorkloadModelLinks().getLink() != null) && Objects.equals(first.getWorkloadModelLinks().getLink(), second.getWorkloadModelLinks().getLink());
		overlap |= (first.getLoadTestLinks().getLink() != null) && Objects.equals(first.getLoadTestLinks().getLink(), second.getLoadTestLinks().getLink());

		return overlap;
	}

}
