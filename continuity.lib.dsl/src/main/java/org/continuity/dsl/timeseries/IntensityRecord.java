package org.continuity.dsl.timeseries;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Holds information about intensity and context values at one date.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "timestamp", "intensity", "context" })
public class IntensityRecord {

	public static final String PATH_TIMESTAMP = "timestamp";

	public static final String PATH_CONTEXT_NUMERIC = "context.numeric";

	public static final String PATH_CONTEXT_STRING = "context.string";

	public static final String PATH_CONTEXT_BOOLEAN = "context.boolean";

	private long timestamp;

	@JsonInclude(Include.NON_EMPTY)
	@JsonDeserialize(as = TreeMap.class)
	private Map<String, Long> intensity;

	@JsonInclude(Include.NON_NULL)
	private ContextRecord context;

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Map<String, Long> getIntensity() {
		return intensity;
	}

	public void setIntensity(Map<String, Long> intensity) {
		this.intensity = intensity;
	}

	public ContextRecord getContext() {
		return context;
	}

	public void setContext(ContextRecord context) {
		this.context = context;
	}

}
