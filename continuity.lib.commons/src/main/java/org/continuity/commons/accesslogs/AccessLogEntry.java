package org.continuity.commons.accesslogs;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents one line of an access log that can be parsed from a log line and can be stored as CSV
 * row.
 *
 * @author Henning Schulz
 *
 */
public class AccessLogEntry {

	private static final Logger LOGGER = LoggerFactory.getLogger(AccessLogEntry.class);

	public static final String CSV_DELIMITER = ",";

	/**
	 * "endpoint","timestamp","responseTime","host","remoteName","user","requestMethod","path","protocol","statusCode","responseBytes","requestParameters","urlParameters","body"
	 */
	public static final String CSV_HEADER = Arrays.asList("endpoint", "timestamp", "responseTime", "host", "remoteName", "user", "requestMethod", "path", "protocol", "statusCode", "responseBytes",
			"requestParameters", "urlParameters", "body").stream().collect(Collectors.joining(CSV_DELIMITER));

	/**
	 * host remoteName user [accessTime] "requestMethod path protocol" statusCode responseBytes
	 * "referer" "userAgent" responseTime
	 */
	public static final String DEFAULT_REGEX = "([^ ]+) ([^ ]+) ([^ ]+) \\[([^\\]]+)\\] \"([A-Z]+) ([^\"]+) ([^\"]+)\" (\\d+) (-|\\d+)(?: \"([^\"]+)\")?(?: \"([^\"]+)\")?(?: (\\d+))?";

	private static final Pattern DEFAULT_PATTERN = Pattern.compile(DEFAULT_REGEX);

	private static final String[] FIELDS = { "host", "remoteName", "user", "accessTime", "requestMethod", "path", "protocol", "statusCode", "responseBytes", "referer", "userAgent", "responseTime" };

	private static final String DEFAULT_DATE_FORMAT = "dd/MMM/yyyy:HH:mm:ss Z";

	private String endpoint;

	private String host;

	private String remoteName;

	private String user;

	private String accessTime;

	private String requestMethod;

	private String path;

	private List<ParameterRecord> requestParameters;

	private List<ParameterRecord> urlParameters;

	private List<ParameterRecord> formParameters;

	private String body;

	private String protocol;

	private int statusCode;

	private long responseBytes;

	private String referer;

	private String userAgent;

	private long responseTime;

	public static AccessLogEntry fromLogLine(String line) {
		return fromLogLine(line, DEFAULT_PATTERN);
	}

	public static AccessLogEntry fromLogLine(String line, Pattern pattern) {
		Matcher matcher = pattern.matcher(line);

		if (matcher.find()) {
			AccessLogEntry entry = new AccessLogEntry();

			for (int group = 1; group <= matcher.groupCount(); group++) {
				switch (FIELDS[group - 1]) {
				case "host":
					entry.setHost(matcher.group(group));
					break;
				case "remoteName":
					entry.setRemoteName(matcher.group(group));
					break;
				case "user":
					entry.setUser(matcher.group(group));
					break;
				case "accessTime":
					entry.setAccessTime(matcher.group(group));
					break;
				case "requestMethod":
					entry.setRequestMethod(matcher.group(group));
					break;
				case "path":
					String[] pathAndQuery = matcher.group(group).split("\\?");
					entry.setPath(pathAndQuery[0]);

					if (pathAndQuery.length > 1) {
						entry.setRequestParameters(Arrays.stream(pathAndQuery[1].split("&")).map(ParameterRecord::fromString).collect(Collectors.toList()));
					}
					break;
				case "protocol":
					entry.setProtocol(matcher.group(group));
					break;
				case "statusCode":
					entry.setStatusCode(Integer.parseInt(matcher.group(group)));
					break;
				case "responseBytes":
					String reponseBytesValue = matcher.group(group);
					if ("-".equals(reponseBytesValue)) {
						entry.setResponseBytes(0);
					} else {
						entry.setResponseBytes(Long.parseLong(reponseBytesValue));
					}
					break;
				case "referer":
					entry.setReferer(matcher.group(group));
					break;
				case "userAgent":
					entry.setUserAgent(matcher.group(group));
					break;
				case "responseTime":
					String responseTime = matcher.group(group);
					entry.setResponseTime(responseTime == null ? 0 : Long.parseLong(responseTime));
					break;
				default:
					break;
				}
			}

			return entry;
		} else {
			LOGGER.warn("Cannot parse log line '{}'!", line);
			return null;
		}
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getRemoteName() {
		return remoteName;
	}

	public void setRemoteName(String remoteName) {
		this.remoteName = remoteName;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getAccessTime() {
		return accessTime;
	}

	public Date getAccessTimeAsDate() throws ParseException {
		return DateUtils.parseDate(accessTime, Locale.ENGLISH, DEFAULT_DATE_FORMAT);
	}

	public void setAccessTime(String accessTime) {
		this.accessTime = accessTime;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<ParameterRecord> getRequestParameters() {
		return requestParameters;
	}

	public void setRequestParameters(List<ParameterRecord> requestParameters) {
		this.requestParameters = requestParameters;
	}

	public String getPathAndQuery() {
		if ((requestParameters == null) || requestParameters.isEmpty()) {
			return path;
		} else {
			String query = requestParameters.stream().map(ParameterRecord::toString).collect(Collectors.joining("&"));
			return new StringBuilder().append(path).append("?").append(query).toString();
		}
	}

	public List<ParameterRecord> getUrlParameters() {
		return urlParameters;
	}

	public void setUrlParameters(List<ParameterRecord> urlParameters) {
		this.urlParameters = urlParameters;
	}

	public List<ParameterRecord> getFormParameters() {
		return formParameters;
	}

	public void setFormParameters(List<ParameterRecord> formParameters) {
		this.formParameters = formParameters;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public long getResponseBytes() {
		return responseBytes;
	}

	public void setResponseBytes(long responseBytes) {
		this.responseBytes = responseBytes;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 *
	 * @return Response time in microseconds.
	 */
	public long getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(long responseTime) {
		this.responseTime = responseTime;
	}

	/**
	 * @see #CSV_HEADER
	 * @return
	 */
	public String toCsvRow() {
		StringBuilder row = new StringBuilder();

		row = row.append(formatForCsv(endpoint)).append(CSV_DELIMITER);
		row = row.append(formatForCsv(accessTime)).append(CSV_DELIMITER);
		row = row.append(responseTime).append(CSV_DELIMITER);
		row = row.append(formatForCsv(host)).append(CSV_DELIMITER);
		row = row.append(formatForCsv(remoteName)).append(CSV_DELIMITER);
		row = row.append(formatForCsv(user)).append(CSV_DELIMITER);
		row = row.append(formatForCsv(requestMethod)).append(CSV_DELIMITER);
		row = row.append(formatForCsv(path)).append(CSV_DELIMITER);
		row = row.append(formatForCsv(protocol)).append(CSV_DELIMITER);
		row = row.append(statusCode).append(CSV_DELIMITER);
		row = row.append(responseBytes).append(CSV_DELIMITER);
		row = row.append(formatParameters(requestParameters)).append(CSV_DELIMITER);
		row = row.append(formatParameters(urlParameters)).append(CSV_DELIMITER);
		row = row.append(formatForCsv(body));

		return row.toString();
	}

	private String formatForCsv(String entry) {
		if (entry == null) {
			return "";
		} else {
			return entry;
		}
	}

	private String formatParameters(List<ParameterRecord> params) {
		if ((params == null) || params.isEmpty()) {
			return "";
		} else {
			return formatForCsv(params.stream().map(ParameterRecord::toString).collect(Collectors.joining("&")));
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessTime, body, endpoint, formParameters, host, path, protocol, referer, remoteName, requestMethod, requestParameters, responseBytes, responseTime, statusCode,
				urlParameters, user, userAgent);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AccessLogEntry)) {
			return false;
		}

		AccessLogEntry other = (AccessLogEntry) obj;
		return Objects.equals(accessTime, other.accessTime) && Objects.equals(body, other.body) && Objects.equals(endpoint, other.endpoint) && Objects.equals(formParameters, other.formParameters)
				&& Objects.equals(host, other.host) && Objects.equals(path, other.path) && Objects.equals(protocol, other.protocol) && Objects.equals(referer, other.referer)
				&& Objects.equals(remoteName, other.remoteName) && Objects.equals(requestMethod, other.requestMethod) && Objects.equals(requestParameters, other.requestParameters)
				&& (responseBytes == other.responseBytes) && (responseTime == other.responseTime) && (statusCode == other.statusCode) && Objects.equals(urlParameters, other.urlParameters)
				&& Objects.equals(user, other.user) && Objects.equals(userAgent, other.userAgent);
	}

}
