package org.continuity.commons.accesslogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;

/**
 *
 * @author Henning Schulz
 *
 */
public abstract class AbstractAccessLogsConsumer {

	private final RequestUriMapper mapper;

	private final Path pathToAccessLogs;

	private Set<String> ignoredRequests = new LinkedHashSet<>();

	private Pattern pattern;

	private Predicate<AccessLogEntry> filter;

	public AbstractAccessLogsConsumer(Application application, Path pathToAccessLogs) {
		this.mapper = new RequestUriMapper(application);
		this.pathToAccessLogs = pathToAccessLogs;
	}

	/**
	 * Sets the regular expression. If this method is not called,
	 * {@value AccessLogEntry#DEFAULT_REGEX} will be used.
	 *
	 * @param regex
	 *            The regular expression used to extract the request properties. There should be one
	 *            capture group per property.
	 */
	public void setRegex(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	/**
	 * Sets a filter that will be applied on the logs.
	 *
	 * @param filter
	 */
	public void setFilter(Predicate<AccessLogEntry> filter) {
		this.filter = filter;
	}

	public void consume() throws IOException {
		init();

		BufferedReader reader = Files.newBufferedReader(pathToAccessLogs);

		String line = reader.readLine();

		while (line != null) {
			processLine(line);
			line = reader.readLine();
		}

		finalize();
	}

	private void processLine(String line) throws IOException {
		if (pattern == null) {
			setRegex(AccessLogEntry.DEFAULT_REGEX);
		}

		AccessLogEntry logEntry = AccessLogEntry.fromLogLine(line, pattern);

		if ((logEntry != null) && ((filter == null) || filter.test(logEntry))) {
			HttpEndpoint endpoint = mapper.map(logEntry.getPath(), logEntry.getRequestMethod());

			if (endpoint == null) {
				ignoredRequests.add(logEntry.getRequestMethod() + " " + logEntry.getPathAndQuery());
			} else {
				process(logEntry, endpoint);
			}
		}
	}

	protected abstract void init() throws IOException;

	protected abstract void process(AccessLogEntry logEntry, HttpEndpoint endpoint) throws IOException;

	@Override
	protected abstract void finalize() throws IOException;

	public Set<String> getIgnoredRequests() {
		return ignoredRequests;
	}

}
