package org.continuity.commons.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.continuity.api.entities.order.ServiceSpecification;
import org.continuity.api.rest.RestApi;
import org.continuity.idpa.AppId;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.springframework.web.client.RestTemplate;

/**
 * Common utils to be used during tailoring.
 *
 * @author Henning Schulz, Tobias Angerstein
 *
 */
public class TailoringUtils {

	private TailoringUtils() {
	}

	/**
	 * Determines a list of target hosts from a list of {@link ServiceSpecification}.
	 *
	 * @param aid
	 *            The app-id. The services will be resolved when requesting the application models.
	 * @param services
	 *            The services to be processed.
	 * @param restTemplate
	 *            The rest template to use for retrieving application models.
	 * @return The list of target hosts.
	 */
	public static Collection<String> getTargetHostNames(AppId aid, List<ServiceSpecification> services, RestTemplate restTemplate) {
		return services.stream()
				.map(t -> restTemplate.getForObject(RestApi.Idpa.Application.GET.requestUrl(aid.withService(t.getService())).withQueryIfNotEmpty("version", t.getVersion().toString()).get(),
						Application.class))
				.map(Application::getEndpoints).flatMap(List::stream).map(endp -> (HttpEndpoint) endp).map(HttpEndpoint::getDomain).collect(Collectors.toSet());
	}

	/**
	 * Retrieves all application models for a list of {@link ServiceSpecification}.
	 *
	 * @param aid
	 *            The app-id. The services will be resolved when requesting the application models.
	 * @param services
	 *            The services to be processed.
	 * @param restTemplate
	 *            The rest template to use for retrieving application models.
	 * @return The list of {@link Application}.
	 */
	public static Collection<Application> getServiceApplicationModels(AppId aid, List<ServiceSpecification> services, RestTemplate restTemplate) {
		return services.stream()
				.map(t -> restTemplate.getForObject(RestApi.Idpa.Application.GET.requestUrl(aid.withService(t.getService())).withQueryIfNotEmpty("version", t.getVersion().toString()).get(),
						Application.class))
				.collect(Collectors.toList());
	}

	/**
	 * Determines whether tailoring is to be applied based on a list of
	 * {@link ServiceSpecification}.
	 *
	 * @param services
	 *            The services to be processed.
	 * @return {@code true} if tailoring is to be done, regardless of the tailoring approach.
	 */
	public static boolean doTailoring(List<ServiceSpecification> services) {
		return (services != null) && (services.size() > 0) && !((services.size() == 1) && AppId.SERVICE_ALL.equals(services.get(0).getService()));
	}

}
