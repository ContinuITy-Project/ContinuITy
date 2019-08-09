package org.continuity.api.entities.config.cobra;

import java.time.Duration;

import org.continuity.api.entities.config.cobra.CobraConfiguration.DurationToStringConverter;
import org.continuity.api.entities.config.cobra.CobraConfiguration.StringToDurationConverter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 *
 * @author Henning Schulz
 *
 */
public class SessionsConfiguration {

	public static final Duration DEFAULT_MAX_SESSION_PAUSE = Duration.ofMinutes(30);

	@JsonSerialize(converter = DurationToStringConverter.class)
	@JsonDeserialize(converter = StringToDurationConverter.class)
	private Duration timeout = DEFAULT_MAX_SESSION_PAUSE;

	@JsonProperty("hash-id")
	private boolean hashId = false;

	@JsonProperty("ignore-redirects")
	private boolean ignoreRedirects = true;

	private boolean omit = false;

	/**
	 *
	 * @return The maximum time of inactivity a session can have. After that, a new session starts.
	 */
	public Duration getTimeout() {
		return timeout;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	/**
	 *
	 * @return Whether session IDs (only those extracted from client IPs or related information)
	 *         should be hashed for privacy concerns.
	 */
	public boolean isHashId() {
		return hashId;
	}

	public void setHashId(boolean hashSessionId) {
		this.hashId = hashSessionId;
	}

	/**
	 *
	 * @return Whether requests following a request with a redirect response code should be ignored.
	 *         Defaults to {@code true}.
	 */
	public boolean isIgnoreRedirects() {
		return ignoreRedirects;
	}

	public void setIgnoreRedirects(boolean ignoreRedirects) {
		this.ignoreRedirects = ignoreRedirects;
	}

	/**
	 *
	 * @return Whether the automated grouping into sessions should be omitted. Defaults to
	 *         {@code false}. If set to {@code true}, no automated clustering will be done, either.
	 */
	public boolean isOmit() {
		return omit;
	}

	public void setOmit(boolean omitSessionClustering) {
		this.omit = omitSessionClustering;
	}

}
