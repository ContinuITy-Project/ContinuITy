package org.continuity.request.rates.transform;

import java.util.Collection;
import java.util.stream.Collectors;

import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.visitor.FindBy;
import org.continuity.request.rates.entities.RequestRecord;

/**
 * Labels the requests according to the service application models.
 *
 * @author Henning Schulz
 *
 */
public class ModularizingRequestRatesCalculator extends RequestRatesCalculator {

	private final Collection<Application> applications;

	private final Collection<RequestUriMapper> uriMappers;

	public ModularizingRequestRatesCalculator(Collection<Application> applications) {
		this.applications = applications;
		this.uriMappers = applications.stream().map(RequestUriMapper::new).collect(Collectors.toList());
	}

	@Override
	protected HttpEndpoint mapToEndpoint(RequestRecord record) {
		HttpEndpoint endpoint = null;

		if (record.getName() != null) {
			for (Application app : applications) {
				endpoint = FindBy.findById(record.getName(), HttpEndpoint.class).in(app).getFound();
			}
		}

		if ((endpoint == null) && (record.getPath() != null)) {
			for (RequestUriMapper mapper : uriMappers) {
				HttpEndpoint found = mapper.map(record.getPath(), record.getMethod());

				if ((found != null)
						&& found.getDomain().equals(record.getDomain())
						&& found.getPort().equals(record.getPort())) {
					endpoint = found;
				}
			}
		}

		return endpoint;
	}

	@Override
	public boolean useNames() {
		return (applications == null) || applications.isEmpty();
	}

}
