package org.continuity.idpa.annotation.extracted;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.application.Endpoint;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

public class EndpointOrInput {

	@JsonProperty("endpoint")
	@JsonInclude(Include.NON_NULL)
	private WeakReference<Endpoint<?>> endpoint;

	@JsonProperty("response-key")
	@JsonInclude(Include.NON_NULL)
	private String responseKey;

	@JsonProperty(value = "input", required = false)
	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	@JsonInclude(Include.NON_NULL)
	private Input input;

	/**
	 * Only used for parsing.
	 */
	@JsonInclude(Include.NON_NULL)
	private String rawInputId;

	@JsonIgnore
	public static EndpointOrInput endpoint(WeakReference<Endpoint<?>> endpoint, String responseKey) {
		EndpointOrInput eoi = new EndpointOrInput();
		eoi.setEndpoint(endpoint);
		eoi.setResponseKey(responseKey);
		return eoi;
	}

	@JsonIgnore
	public static EndpointOrInput endpoint(WeakReference<Endpoint<?>> endpoint) {
		return endpoint(endpoint, null);
	}

	@JsonIgnore
	public static EndpointOrInput input(Input input) {
		EndpointOrInput eoi = new EndpointOrInput();
		eoi.setInput(input);
		return eoi;
	}

	public WeakReference<Endpoint<?>> getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(WeakReference<Endpoint<?>> endpoint) {
		this.endpoint = endpoint;
	}

	/**
	 * Gets the key. Can be used to specify a specific response, e.g., the header or body of an HTTP
	 * response.
	 *
	 * @return {@link #key} The key.
	 */
	public String getResponseKey() {
		return responseKey;
	}

	public void setResponseKey(String responseKey) {
		this.responseKey = responseKey;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public String getRawInputId() {
		return rawInputId;
	}

	public void setRawInputId(String rawInputId) {
		this.rawInputId = rawInputId;
	}

	@JsonIgnore
	public boolean isEndpoint() {
		return endpoint != null;
	}

	@JsonIgnore
	public boolean isInput() {
		return !isEndpoint() && ((rawInputId != null) || (input != null));
	}

}
