package org.continuity.commons.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.continuity.api.entities.config.ModularizationOptions;
import org.continuity.api.rest.RestApi.IdpaApplication;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.springframework.web.client.RestTemplate;

/**
 * Common utils to be used during modularization.
 *
 * @author Henning Schulz, Tobias Angerstein
 *
 */
public class ModularizationUtils {

	private ModularizationUtils() {
	}

	/**
	 * Determines a list of target hosts from the services map of
	 * {@link ModularizationOptions#getServices()}.
	 *
	 * @param services
	 *            The services map to be processed.
	 * @param restTemplate
	 *            The rest template to use for retrieving application models.
	 * @return The list of target hosts.
	 */
	public static Collection<String> getTargetHostNames(Map<String, String> services, RestTemplate restTemplate) {
		if (!services.values().contains("undefined")) {
			return services.values();
		} else {
			return services.keySet().stream().map(t -> restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(t).get(), Application.class)).map(Application::getEndpoints)
					.flatMap(List::stream).map(endp -> (HttpEndpoint) endp)
					.map(HttpEndpoint::getDomain)
					.collect(Collectors.toSet());
		}
	}

	/**
	 * Retrieves all application models for the services map of
	 * {@link ModularizationOptions#getServices()}.
	 *
	 * @param services
	 *            The services map to be processed.
	 * @param restTemplate
	 *            The rest template to use for retrieving application models.
	 * @return The list of {@link Application}.
	 */
	public static Collection<Application> getServiceApplicationModels(Map<String, String> services, RestTemplate restTemplate) {
		return services.keySet().stream().map(t -> restTemplate.getForObject(IdpaApplication.Application.GET.requestUrl(t).get(), Application.class)).collect(Collectors.toList());
	}

}
