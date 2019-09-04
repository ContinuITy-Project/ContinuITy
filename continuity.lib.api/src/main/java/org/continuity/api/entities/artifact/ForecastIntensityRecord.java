package org.continuity.api.entities.artifact;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Henning Schulz
 *
 */
public class ForecastIntensityRecord {

	private static final String KEY_TIMESTAMP = "timestamp";

	@JsonValue
	private Map<String, Double> content;

	@JsonCreator
	public ForecastIntensityRecord(Map<String, Double> content) {
		this.content = content;
	}

	public Map<String, Double> getContent() {
		return content;
	}

	public void setContent(Map<String, Double> content) {
		this.content = content;
	}

	public long getTimestamp() {
		return content.get(KEY_TIMESTAMP).longValue();
	}

	public Set<String> getGroups() {
		Set<String> groups = new HashSet<>(content.keySet());
		groups.remove(KEY_TIMESTAMP);
		return groups;
	}

	public double getIntensity(String group) {
		return content.get(group);
	}

}
