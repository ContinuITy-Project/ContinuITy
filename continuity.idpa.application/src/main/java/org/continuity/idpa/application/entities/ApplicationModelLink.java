package org.continuity.idpa.application.entities;

import java.util.Date;

import org.continuity.api.entities.ApiFormats;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;

/**
 * @author Henning Schulz
 *
 */
public class ApplicationModelLink extends LinkExchangeModel {

	public ApplicationModelLink(String applicationName, String tag, Date beforeDate) {
		setApplicationLink(RestApi.IdpaApplication.Application.GET.requestUrl(tag).withHost(applicationName).withoutProtocol().get());
		setDeltaLink(RestApi.IdpaApplication.Application.GET_DELTA.requestUrl(tag).withHost(applicationName).withQuery("since", ApiFormats.DATE_FORMAT.format(beforeDate)).withoutProtocol().get());
		setTag(tag);
	}

}
