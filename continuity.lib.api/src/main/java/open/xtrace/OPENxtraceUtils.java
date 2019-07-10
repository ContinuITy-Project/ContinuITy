package open.xtrace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.api.core.callables.HTTPRequestProcessing;
import org.spec.research.open.xtrace.api.core.callables.NestingCallable;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.RemoteInvocationImpl;
import org.spec.research.open.xtrace.dflt.impl.serialization.realizations.JsonOPENxtraceDeserializer;
import org.spec.research.open.xtrace.dflt.impl.serialization.realizations.JsonOPENxtraceSerializer;
import org.springframework.web.client.RestTemplate;

public class OPENxtraceUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(OPENxtraceUtils.class);

	private static final String JSESSIONID = "JSESSIONID";

	private static final String MD_SID = "md.sid";

	private static final String COOKIES_KEY = "cookie";

	private static JsonOPENxtraceSerializer serializer;

	private static JsonOPENxtraceDeserializer deserializer;

	private static JsonOPENxtraceSerializer initAndGetSerializer() {
		if (serializer == null) {
			synchronized (OPENxtraceUtils.class) {
				if (serializer == null) {
					serializer = new JsonOPENxtraceSerializer();
				}
			}
		}

		return serializer;
	}

	private static JsonOPENxtraceDeserializer initAndGetDeserializer() {
		if (deserializer == null) {
			synchronized (OPENxtraceUtils.class) {
				if (deserializer == null) {
					deserializer = new JsonOPENxtraceDeserializer();
				}
			}
		}

		return deserializer;
	}

	/**
	 * Deserializes a given String into a list of traces
	 *
	 * @return an Iterable of Traces
	 */
	public static List<Trace> deserializeIntoTraceList(String openxtrace) {
		JsonOPENxtraceDeserializer deserializer = initAndGetDeserializer();

		try {
			return deserializer.deserialize(openxtrace);
		} catch (IOException e) {
			LOGGER.error("Could not deserialize trace list! Returning null.", e);
			return null;
		}
	}

	/**
	 * Deserializes a single JSON string to a {@link Trace}.
	 *
	 * @param traceJson
	 *            The JSON representation of the trace.
	 * @return The deserializes trace.
	 */
	public static Trace deserializeToTrace(String traceJson) {
		JsonOPENxtraceDeserializer deserializer = initAndGetDeserializer();
		try {
			return deserializer.deserialize(new StringBuilder().append("[").append(traceJson).append("]").toString()).get(0);
		} catch (IOException e) {
			LOGGER.error("Could not deserialize trace! Returning null.", e);
			return null;
		}
	}

	public static String serializeTraceListToJsonString(List<Trace> traces) {
		JsonOPENxtraceSerializer serializer = initAndGetSerializer();
		return serializer.serialize(traces);
	}

	/**
	 * Serializes a single trace into a string.
	 *
	 * @param trace
	 * @return
	 */
	public static String serializeTraceToJsonString(Trace trace) {
		JsonOPENxtraceSerializer serializer = initAndGetSerializer();
		String json = serializer.serialize(Collections.singletonList(trace)).trim();
		return json.substring(1, json.length() - 1);
	}

	/**
	 * Extracts all httpRequestProcessingImpl callables
	 *
	 * @param traces
	 * @param sortedHTTPInvocCallables
	 */
	public static List<HTTPRequestProcessingImpl> extractHttpRequestCallables(Iterable<Trace> traces) {
		List<HTTPRequestProcessingImpl> httpCallables = new ArrayList<HTTPRequestProcessingImpl>();
		for (Trace trace : traces) {
			if ((null != trace.getRoot()) && (null != trace.getRoot().getRoot())) {
				httpCallables.addAll(diveForHTTPRequestProcessingCallable(trace.getRoot().getRoot()));
			}
		}
		return httpCallables;
	}

	/**
	 * Returns the {@link HTTPRequestProcessingImpl} object of the corresponding
	 * {@link RemoteInvocationImpl} which is on the highest level.
	 *
	 * @param callable
	 *            The root callable
	 * @return List of {@link HTTPRequestProcessingImpl}
	 *
	 *         TODO: Move this and the depending methods to continuITY api, because it is used by
	 *         two different microservices
	 */
	public static List<HTTPRequestProcessingImpl> diveForHTTPRequestProcessingCallable(Callable callable) {
		List<HTTPRequestProcessingImpl> httpRequestProcessingCallables = new ArrayList<HTTPRequestProcessingImpl>();
		if (null != callable) {
			LinkedBlockingQueue<Callable> callables = new LinkedBlockingQueue<Callable>();
			callables.add(callable);
			while (!callables.isEmpty()) {
				Callable currentCallable = callables.poll();
				if (currentCallable instanceof HTTPRequestProcessingImpl) {
					httpRequestProcessingCallables.add((HTTPRequestProcessingImpl) currentCallable);
				} else if ((currentCallable instanceof NestingCallable) && httpRequestProcessingCallables.isEmpty()) {
					callables.addAll(((NestingCallable) currentCallable).getCallees());
				} else if ((currentCallable instanceof RemoteInvocationImpl) && ((RemoteInvocationImpl) currentCallable).getTargetSubTrace().isPresent()
						&& (null != ((RemoteInvocationImpl) currentCallable).getTargetSubTrace().get().getRoot())) {
					callables.add(((RemoteInvocationImpl) currentCallable).getTargetSubTrace().get().getRoot());
				}
			}
		}
		return httpRequestProcessingCallables;
	}

	/**
	 * Fetches traces from server and deserialize it
	 *
	 * @return list of traces
	 */
	public static List<Trace> getOPENxtraces(String url, RestTemplate restTemplate) {
		String openxtrace = restTemplate.getForObject(url, String.class);
		List<Trace> traces = OPENxtraceUtils.deserializeIntoTraceList(openxtrace);
		return (traces);
	}

	/**
	 * Extracts the session id from cookies
	 *
	 * @param cookieString
	 *            the cookies of the request
	 * @return the session id
	 */
	public static String extractSessionIdFromCookies(String cookieString) {
		Map<String, String> cookies = splitCookies(cookieString);

		String sessionId = cookies.get(JSESSIONID);

		if (sessionId == null) {
			sessionId = cookies.get(MD_SID);
		}

		return sessionId;
	}

	/**
	 * Extracts the session id from cookies
	 *
	 * @param cookieString
	 *            the cookies of the request
	 * @return the session id
	 */
	public static String extractSessionIdFromCookies(HTTPRequestProcessing callable) {
		if (callable.getHTTPHeaders().isPresent() && callable.getHTTPHeaders().get().containsKey("cookie")) {
			return extractSessionIdFromCookies(callable.getHTTPHeaders().get().get("cookie"));
		} else {
			return null;
		}
	}

	public static void setSessionId(HTTPRequestProcessingImpl proc, String sessionId) {
		if (!proc.getHTTPHeaders().isPresent()) {
			proc.setHTTPHeaders(new HashMap<>());
		}

		if (proc.getHTTPHeaders().get().containsKey(COOKIES_KEY)) {
			Map<String, String> cookies = splitCookies(proc.getHTTPHeaders().get().get(COOKIES_KEY));

			if (cookies.containsKey(MD_SID)) {
				cookies.put(MD_SID, sessionId);
			} else {
				cookies.put(JSESSIONID, sessionId);
			}

			proc.getHTTPHeaders().get().put(COOKIES_KEY, mergeCookies(cookies));
		} else {
			proc.getHTTPHeaders().get().put(COOKIES_KEY, JSESSIONID + "=" + sessionId);
		}

	}

	private static Map<String, String> splitCookies(String cookieString) {
		LinkedHashMap<String, String> cookies = new LinkedHashMap<>();

		if (cookieString != null) {
			Arrays.stream(cookieString.split(";")).map(String::trim).map(s -> s.split("=")).forEach(s -> cookies.put(s[0], s.length > 1 ? s[1] : null));
		} else {
			LOGGER.warn("Cannot split the cookies! The cookieString is null!");
		}

		return cookies;
	}

	private static String mergeCookies(Map<String, String> cookies) {
		StringBuilder builder = new StringBuilder();

		cookies.entrySet().forEach(entry -> {
			builder.append(entry.getKey());

			if (entry.getValue() != null) {
				builder.append("=");
				builder.append(entry.getValue());
			}

			builder.append(";");
		});

		return builder.substring(0, builder.length() - 1).toString();
	}

}
