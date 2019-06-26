package org.continuity.request.rates.transform;

import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.visitor.FindBy;
import org.continuity.request.rates.entities.RequestRecord;

public class SimpleRequestRatesCalculator extends RequestRatesCalculator {

	private final Application application;

	private final RequestUriMapper uriMapper;

	public SimpleRequestRatesCalculator() {
		this(null);
	}

	public SimpleRequestRatesCalculator(Application application) {
		this.application = application;
		this.uriMapper = application == null ? null : new RequestUriMapper(application);
	}

	@Override
	protected HttpEndpoint mapToEndpoint(RequestRecord record) {
		HttpEndpoint endpoint = null;

		if (record.getName() != null) {
			endpoint = FindBy.findById(record.getName(), HttpEndpoint.class).in(application).getFound();
		}

		if ((endpoint == null) && (record.getPath() != null)) {
			endpoint = uriMapper.map(record.getPath(), record.getMethod());
		}

		return endpoint;
	}

	@Override
	public boolean useNames() {
		return application == null;
	}

}
