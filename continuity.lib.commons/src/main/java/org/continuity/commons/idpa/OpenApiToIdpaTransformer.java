package org.continuity.commons.idpa;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.continuity.commons.utils.StringUtils;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;

/**
 * Transforms {@link Swagger} objects into {@link Application} instances.
 *
 * @author Henning Schulz
 *
 */
public class OpenApiToIdpaTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiToIdpaTransformer.class);

	/**
	 * Transforms the specified {@link Swagger} object into a {@link Application};
	 *
	 * @param swagger
	 *            The swagger model to be transformed.
	 * @return The generated application model.
	 */
	public Application transform(Swagger swagger) {
		LOGGER.info("Transforming the swagger model {} to an application model.", swagger.getInfo().getTitle());

		Application system = new Application();

		final String name = swagger.getInfo().getTitle();

		if (name != null) {
			system.setId(name.replace(" ", "_"));
		}

		system.setEndpoints(extractInterfaces(swagger));

		return system;
	}

	private List<Endpoint<?>> extractInterfaces(Swagger swagger) {
		final String host = swagger.getHost();
		final String basePath = swagger.getBasePath();
		String prot = extractProtocol(swagger.getSchemes());

		if (prot == null) {
			LOGGER.info("No global protocol specified. Using http.");
			prot = "http";
		}

		final String protocol = prot;

		String domain;
		String port;
		if ((host != null) && host.contains(":")) {
			String[] split = host.split("\\:");
			domain = split[0];
			port = split[1];
		} else {
			domain = host;
			port = "80";
		}

		return swagger.getPaths().entrySet().stream().map(entry -> createInterfacesFromPath(entry, domain, port, basePath, protocol)).flatMap(List::stream).collect(Collectors.toList());
	}

	private List<Endpoint<?>> createInterfacesFromPath(Entry<String, Path> pathEntry, String host, String port, String basePath, String protocol) {
		final String path = ("/".equals(basePath) ? "" : basePath) + pathEntry.getKey();

		List<Endpoint<?>> interfaces = pathEntry.getValue().getOperationMap().entrySet().stream().map(this::transformToInterface).map(interf -> {
			interf.setDomain(host);
			interf.setPort(port);
			interf.setPath(path);

			if (interf.getProtocol() == null) {
				interf.setProtocol(protocol);
			}

			if (interf.getId().startsWith("!")) {
				interf.setId(StringUtils.formatAsId(false, path, interf.getId().substring(1)));
			}

			setParameterIds(interf);

			return interf;
		}).collect(Collectors.toList());

		return interfaces;
	}

	private HttpEndpoint transformToInterface(Entry<HttpMethod, Operation> operationEntry) {
		String method = operationEntry.getKey().toString();
		Operation operation = operationEntry.getValue();

		HttpEndpoint interf = new HttpEndpoint();

		if (operation.getConsumes() != null) {
			List<String> headers = operation.getConsumes().stream().map(type -> Arrays.asList("Accept: " + type, "Content-Type: " + type)).flatMap(List::stream).collect(Collectors.toList());
			interf.setHeaders(headers);
		}

		interf.setMethod(method);
		interf.setProtocol(extractProtocol(operation.getSchemes()));

		String id = operation.getOperationId();

		if (id != null) {
			interf.setId(id);
		} else {
			interf.setId("!" + method);
		}

		interf.setParameters(operation.getParameters().stream().map(this::convertToParameter).collect(Collectors.toList()));

		return interf;
	}

	private HttpParameter convertToParameter(Parameter swaggerParam) {
		HttpParameter param = new HttpParameter();

		param.setName(swaggerParam.getName());
		param.setParameterType(transformToParameterType(swaggerParam.getIn()));

		return param;
	}

	private void setParameterIds(HttpEndpoint interf) {
		final Set<String> ids = new HashSet<>();

		for (HttpParameter param : interf.getParameters()) {
			String id = StringUtils.formatAsId(true, interf.getId(), param.getName(), param.getParameterType().toString());
			String origId = id;
			int i = 2;

			while (ids.contains(id)) {
				id = origId + "_" + i++;
			}

			ids.add(id);
			param.setId(id);
		}
	}

	private String extractProtocol(List<Scheme> schemes) {
		if ((schemes == null) || (schemes.size() == 0)) {
			return null;
		} else if (schemes.size() > 1) {
			return "http";
		} else {
			return schemes.get(0).toString().toLowerCase();
		}
	}

	private HttpParameterType transformToParameterType(String in) {
		switch (in) {
		case "query":
			return HttpParameterType.REQ_PARAM;
		case "path":
			return HttpParameterType.URL_PART;
		case "header":
			return HttpParameterType.HEADER;
		case "form":
			return HttpParameterType.FORM;
		case "body":
			return HttpParameterType.BODY;
		default:
			LOGGER.warn("No 'in' type defined in Open API specification. Using REQ_PARAM.");
			return HttpParameterType.REQ_PARAM;

		}
	}

}
