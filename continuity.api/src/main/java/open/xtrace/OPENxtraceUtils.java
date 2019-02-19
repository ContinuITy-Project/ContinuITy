package open.xtrace;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.json.JSONArray;
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.Callable;
import org.spec.research.open.xtrace.api.core.callables.NestingCallable;
import org.spec.research.open.xtrace.dflt.impl.core.callables.HTTPRequestProcessingImpl;
import org.spec.research.open.xtrace.dflt.impl.core.callables.RemoteInvocationImpl;
import org.spec.research.open.xtrace.dflt.impl.serialization.OPENxtraceDeserializer;
import org.spec.research.open.xtrace.dflt.impl.serialization.OPENxtraceSerializationFactory;
import org.spec.research.open.xtrace.dflt.impl.serialization.OPENxtraceSerializationFormat;
import org.springframework.web.client.RestTemplate;

public class OPENxtraceUtils {

	/**
	 * Deserializes a given String into a list of traces
	 *
	 * @return an Iterable of Traces
	 */
	public static List<Trace> deserializeIntoTraceList(String openxtrace) {
		JSONArray traceArray = new JSONArray(openxtrace);
		StringBuilder traceBuilder = new StringBuilder();

		for (int i = 0; i < traceArray.length(); i++) {
			traceBuilder.append(traceArray.getJSONObject(i));
			traceBuilder.append("\n");
		}
		OPENxtraceDeserializer deserializer = OPENxtraceSerializationFactory.getInstance().getDeserializer(OPENxtraceSerializationFormat.JSON);
		deserializer.setSource(new ByteArrayInputStream(traceBuilder.toString().getBytes()));
		Trace trace = deserializer.readNext();
		List<Trace> traces = new ArrayList<Trace>();
		while (null != trace) {
			traces.add(trace);
			trace = deserializer.readNext();
		}
		return traces;
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
	public static Iterable<Trace> getOPENxtraces(LinkExchangeModel source, RestTemplate restTemplate) {
		String openxtrace = restTemplate.getForObject(source.getMeasurementDataLinks().getLink(), String.class);
		List<Trace> traces = OPENxtraceUtils.deserializeIntoTraceList(openxtrace);
		return (traces);
	}

	/**
	 * Extracts the session id from cookies
	 *
	 * @param cookies
	 *            the cookies of the request
	 * @return the session id
	 */
	public static String extractSessionIdFromCookies(String cookies) {
		String sessionID = null;
		if (cookies != null) {
			int begin = -1;

			if (cookies.indexOf("JSESSIONID=") != -1) {
				begin = cookies.indexOf("JSESSIONID=") + "JSESSIONID=".length();
			} else if (cookies.indexOf("md.sid=") != -1) {
				begin = cookies.indexOf("md.sid=") + "md.sid=".length();
			} else {
				return null;
			}

			sessionID = "";

			while (begin < cookies.length()) {
				char c = cookies.charAt(begin);
				if (c == ';') {
					break;
				}
				sessionID += c;
				begin++;
			}
		}

		return sessionID;
	}
}
