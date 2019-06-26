package org.continuity.orchestrator.entities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class TestingContextMapping {

	@JsonProperty("full-mappings")
	@JsonDeserialize(keyUsing = SetKeyDeserializer.class)
	private Map<Set<String>, Set<String>> fullMappings = new HashMap<>();

	@JsonProperty("individual-mappings")
	private Map<String, Set<Set<String>>> individualMappings = new HashMap<>();

	/**
	 * testing-context (set of strings) => set of artifact IDs
	 *
	 * @return
	 */
	public Map<Set<String>, Set<String>> getFullMappings() {
		if (fullMappings == null) {
			fullMappings = new HashMap<>();
		}

		return fullMappings;
	}

	public void setFullMappings(Map<Set<String>, Set<String>> fullMappings) {
		this.fullMappings = fullMappings;
	}

	/**
	 * One string that occurs in testing-contexts => set of testing-contexts that hold the string
	 *
	 * @return
	 */
	public Map<String, Set<Set<String>>> getIndividualMappings() {
		if (individualMappings == null) {
			individualMappings = new HashMap<>();
		}

		return individualMappings;
	}

	public void setIndividualMappings(Map<String, Set<Set<String>>> individualMappings) {
		this.individualMappings = individualMappings;
	}

	public void addMapping(Set<String> testingContext, String id) {
		addToFullMappings(testingContext, id);
		addToIndividualMappings(testingContext, id);
	}

	private void addToFullMappings(Set<String> testingContext, String id) {
		Set<String> full = fullMappings.get(testingContext);

		if (full == null) {
			full = new HashSet<>();
			fullMappings.put(testingContext, full);
		}

		full.add(id);
	}

	private void addToIndividualMappings(Set<String> testingContext, String id) {
		for (String ctx : testingContext) {
			Set<Set<String>> mapping = individualMappings.get(ctx);

			if (mapping == null) {
				mapping = new HashSet<>();
				individualMappings.put(ctx, mapping);
			}

			mapping.add(testingContext);
		}
	}

}
