package org.continuity.cli.utils;

import java.io.IOException;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class for conveniently building {@link AttributedString} responses.
 *
 * @author Henning Schulz
 *
 */
public class ResponseBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBuilder.class);

	private AttributedStringBuilder builder = new AttributedStringBuilder();

	private ObjectMapper jsonMapper = SpringContext.getBean(ObjectMapper.class, "jsonObjectMapper");

	private ObjectMapper yamlMapper = SpringContext.getBean(ObjectMapper.class);

	/**
	 * Adds an arbitrarily formatted string. More precise methods should be preferred if available.
	 *
	 * @param string
	 * @param style
	 * @return This builder to continue.
	 *
	 * @see #normal(String)
	 * @see #bold(String)
	 * @see #error(String)
	 */
	public ResponseBuilder append(Object string, AttributedStyle style) {
		return append(new AttributedString(string.toString(), style));
	}

	public ResponseBuilder append(ResponseBuilder other) {
		return append(other.build());
	}

	private ResponseBuilder append(AttributedString as) {
		builder.append(as);
		return this;
	}

	public ResponseBuilder normal(Object string) {
		return append(string, AttributedStyle.DEFAULT);
	}

	public ResponseBuilder bold(Object string) {
		return append(string, AttributedStyle.BOLD);
	}

	public ResponseBuilder error(Object string) {
		return append(string, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
	}

	public ResponseBuilder boldError(Object string) {
		return append(string, AttributedStyle.BOLD.foreground(AttributedStyle.RED));
	}

	public ResponseBuilder newline() {
		return append("\n", AttributedStyle.DEFAULT);
	}

	public ResponseBuilder space() {
		return append(" ", AttributedStyle.DEFAULT);
	}

	public ResponseBuilder jsonAsYamlNormal(String json) {
		return normal(transformJsonToYaml(json));
	}

	public ResponseBuilder jsonAsYamlBold(String json) {
		return bold(transformJsonToYaml(json));
	}

	public ResponseBuilder jsonAsYamlError(String json) {
		return error(transformJsonToYaml(json));
	}

	public ResponseBuilder appendStatusCode(HttpStatus status) {
		return error(" ").boldError(status).boldError(" (").boldError(status.getReasonPhrase()).boldError(") ");
	}

	private String transformJsonToYaml(String json) {
		JsonNode parsed;

		try {
			parsed = jsonMapper.readValue(json, JsonNode.class);
		} catch (IOException e) {
			LOGGER.error("Cannot parse JSON!", e);
			return json;
		}

		try {
			return yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
		} catch (JsonProcessingException e) {
			LOGGER.error("Cannot write YAML!", e);
			return json;
		}
	}

	public AttributedString build() {
		return builder.toAttributedString();
	}

}
