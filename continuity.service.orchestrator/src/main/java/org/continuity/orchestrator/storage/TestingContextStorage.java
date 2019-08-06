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
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.commons.storage.AppIdFileStorage;
import org.continuity.commons.storage.ArtifactStorage;
import org.continuity.commons.storage.JsonFileStorage;
import org.continuity.idpa.AppId;
import org.continuity.orchestrator.entities.TestingContextMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestingContextStorage {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestingContextStorage.class);

	private static final String ARTIFACTS_DIR = "artifacts";

	private static final String MAPPINGS_DIR = "mappings";

	private final ArtifactStorage<ArtifactExchangeModel> artifactStorage;

	private final AppIdFileStorage<TestingContextMapping> mappingStorage;

	public TestingContextStorage(Path storagePath) {
		this.artifactStorage = new JsonFileStorage<ArtifactExchangeModel>(storagePath.resolve(ARTIFACTS_DIR), new ArtifactExchangeModel());
		this.mappingStorage = new AppIdFileStorage<>(storagePath.resolve(MAPPINGS_DIR), TestingContextMapping.class);
	}

	public void store(AppId aid, Set<String> testingContext, ArtifactExchangeModel artifact) throws IOException {
		Pair<String, ArtifactExchangeModel> existing = mergeWithExisting(aid, testingContext, artifact);
		String id;

		if (existing == null) {
			LOGGER.info("Creating a new source entity for testing-context {}", testingContext);
			id = artifactStorage.put(artifact, aid);
		} else {
			LOGGER.info("Updating an existing source entity for testing-context {}", testingContext);
			id = existing.getKey();
			artifact.merge(existing.getValue());
			artifactStorage.putToReserved(id, artifact);
		}

		TestingContextMapping mapping = mappingStorage.read(aid);

		if (mapping == null) {
			mapping = new TestingContextMapping();
		}

		mapping.addMapping(testingContext, id);

		mappingStorage.store(mapping, aid);
	}

	private Pair<String, ArtifactExchangeModel> mergeWithExisting(AppId aid, Set<String> testingContext, ArtifactExchangeModel artifact) throws IOException {
		Map<String, ArtifactExchangeModel> existing = getFull(aid, testingContext);

		for (Map.Entry<String, ArtifactExchangeModel> entry : existing.entrySet()) {
			if (overlap(entry.getValue(), artifact)) {
				return Pair.of(entry.getKey(), entry.getValue());
			}
		}

		return null;
	}

	public Map<Set<String>, Set<ArtifactExchangeModel>> get(AppId aid, Set<String> testingContext, boolean includePartial) throws IOException {
		TestingContextMapping mapping = mappingStorage.read(aid);

		if (mapping == null) {
			return Collections.emptyMap();
		}

		Map<Set<String>, Set<ArtifactExchangeModel>> result = new HashMap<>();

		if (includePartial) {
			for (String ctx : testingContext) {
				Set<Set<String>> indivMapping = mapping.getIndividualMappings().get(ctx);

				if (indivMapping != null) {
					for (Set<String> partialContext : indivMapping) {
						Set<ArtifactExchangeModel> sources = new HashSet<>();

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
			Set<ArtifactExchangeModel> sources = new HashSet<>();
			sources.addAll(getFull(aid, testingContext).values());

			if (!sources.isEmpty()) {
				result.put(testingContext, sources);
			}
		}

		return result;
	}

	private Map<String, ArtifactExchangeModel> getFull(AppId aid, Set<String> testingContext) throws IOException {
		TestingContextMapping mapping = mappingStorage.read(aid);
		Set<String> fullMapping = mapping.getFullMappings().get(testingContext);

		Map<String, ArtifactExchangeModel> sources = new HashMap<>();

		if (fullMapping != null) {
			for (String id : fullMapping) {
				sources.put(id, artifactStorage.get(id));
			}
		}

		return sources;
	}

	private boolean overlap(ArtifactExchangeModel first, ArtifactExchangeModel second) {
		boolean overlap = false;

		overlap |= (first.getTraceLinks().getLink() != null) && Objects.equals(first.getTraceLinks().getLink(), second.getTraceLinks().getLink());
		overlap |= (first.getSessionLinks().getSimpleLink() != null) && Objects.equals(first.getSessionLinks().getSimpleLink(), second.getSessionLinks().getSimpleLink());
		overlap |= (first.getSessionLinks().getExtendedLink() != null) && Objects.equals(first.getSessionLinks().getExtendedLink(), second.getSessionLinks().getExtendedLink());
		overlap |= (first.getWorkloadModelLinks().getLink() != null) && Objects.equals(first.getWorkloadModelLinks().getLink(), second.getWorkloadModelLinks().getLink());
		overlap |= (first.getLoadTestLinks().getLink() != null) && Objects.equals(first.getLoadTestLinks().getLink(), second.getLoadTestLinks().getLink());

		return overlap;
	}

}
