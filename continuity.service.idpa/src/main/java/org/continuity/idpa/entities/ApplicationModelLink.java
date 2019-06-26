package org.continuity.idpa.entities;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.continuity.idpa.VersionOrTimestamp;

/**
 * @author Henning Schulz
 *
 */
public class ApplicationModelLink extends LinkExchangeModel {

	public ApplicationModelLink(String applicationName, String tag, VersionOrTimestamp before) {
		getIdpaLinks().setApplicationLink(RestApi.Idpa.Application.GET.requestUrl(tag).withHost(applicationName).withoutProtocol().get());
		getIdpaLinks().setApplicationDeltaLink(
				RestApi.Idpa.Application.GET_DELTA.requestUrl(tag).withHost(applicationName).withQuery("since", before.toString()).withoutProtocol().get());
		setTag(tag);
	}

}
