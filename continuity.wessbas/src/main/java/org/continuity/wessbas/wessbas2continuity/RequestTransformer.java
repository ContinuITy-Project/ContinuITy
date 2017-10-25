package org.continuity.wessbas.wessbas2continuity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.NotImplementedException;
import org.continuity.workload.dsl.system.HttpInterface;
import org.continuity.workload.dsl.system.HttpParameter;
import org.continuity.workload.dsl.system.HttpParameterType;
import org.continuity.workload.dsl.system.ServiceInterface;

import m4jdsl.HTTPRequest;
import m4jdsl.Property;
import m4jdsl.Request;

/**
 * Transforms a {@link Request} of the WESSBAS model into a {@link ServiceInterface} of the
 * ContinuITy model.
 *
 * @author Henning Schulz
 *
 */
public enum RequestTransformer {

	/**
	 * Consumes {@link HTTPRequest}.
	 */
	HTTP(HTTPRequest.class) {

		@Override
		public ServiceInterface<?> transform(Request request) {
			HttpInterface interf = new HttpInterface();

			for (Property property : request.getProperties()) {
				switch (property.getKey()) {
				case "HTTPSampler.domain":
					interf.setDomain(property.getValue());
					break;
				case "HTTPSampler.port":
					interf.setPort(property.getValue());
					break;
				case "HTTPSampler.path":
					interf.setPath(property.getValue());
					break;
				case "HTTPSampler.method":
					interf.setMethod(property.getValue());
					break;
				case "HTTPSampler.encoding":
					interf.setEncoding(property.getValue());
					break;
				case "HTTPSampler.protocol":
					interf.setProtocol(property.getValue());
					break;
				default:
					// TODO: Log warn message - unknown key
					break;
				}
			}

			for (m4jdsl.Parameter wParam : request.getParameters()) {
				HttpParameter param = new HttpParameter();

				String paramName = wParam.getName();

				if (KEY_BODY.equals(wParam.getName())) {
					param.setParameterType(HttpParameterType.BODY);
				} else if ((wParam.getName() != null) && wParam.getName().startsWith(KEY_URL_PART)) {
					paramName = wParam.getName().split(":")[1];
					param.setParameterType(HttpParameterType.URL_PART);
				} else {
					param.setParameterType(HttpParameterType.REQ_PARAM);
				}

				param.setName(paramName);
				interf.getParameters().add(param);
			}

			// TODO: add headers

			return interf;
		}
	};

	private static final String KEY_BODY = "BODY";
	private static final String KEY_URL_PART = "URL_PART:";

	private static final Map<Class<? extends Request>, RequestTransformer> transformerForType = new HashMap<>();

	private final Class<? extends Request> requestType;

	static {
		for (RequestTransformer transformer : values()) {
			transformerForType.put(transformer.getRequestType(), transformer);
		}
	}

	public static RequestTransformer get(Class<? extends Request> requestType) {
		RequestTransformer transformer = transformerForType.get(requestType);

		if (transformer == null) {
			for (Entry<Class<? extends Request>, RequestTransformer> entry : transformerForType.entrySet()) {
				if (entry.getKey().isAssignableFrom(requestType)) {
					return entry.getValue();
				}
			}

			throw new NotImplementedException("Request type " + requestType + " is not supported!");
		} else {
			return transformer;
		}
	}

	public abstract ServiceInterface<?> transform(Request request);

	private RequestTransformer(Class<? extends Request> requestType) {
		this.requestType = requestType;
	}

	private Class<? extends Request> getRequestType() {
		return this.requestType;
	}

}
