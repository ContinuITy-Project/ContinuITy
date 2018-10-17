package org.continuity.idpa.annotation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Represents an extraction of a value specified by a JSON path such as {@code $.foo.bar}.
 *
 * @author Henning Schulz
 *
 */
@JsonPropertyOrder({ "from", "json-path", "response-key", "match-number", "fallback" })
public class JsonPathExtraction extends AbstractValueExtraction {

	@JsonProperty(value = "json-path")
	private String jsonPath;

	/**
	 * Gets the JSON path, e.g., {@code $.foo.bar}.
	 *
	 * @return The JSON path.
	 */
	public String getJsonPath() {
		return jsonPath;
	}

	/**
	 * Sets the JSON path, e.g., {@code $.foo.bar}.
	 *
	 * @param jsonPath
	 *            The JSON path.
	 */
	public void setJsonPath(String jsonPath) {
		this.jsonPath = jsonPath;
	}

}
