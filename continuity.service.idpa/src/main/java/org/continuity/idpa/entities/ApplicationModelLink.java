package org.continuity.idpa.entities;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;

/**
 * @author Henning Schulz
 *
 */
public class ApplicationModelLink extends LinkExchangeModel {

	public ApplicationModelLink(String applicationName, AppId aid, VersionOrTimestamp before) {
		getIdpaLinks().setApplicationLink(RestApi.Idpa.Application.GET.requestUrl(aid.toString()).withHost(applicationName).withoutProtocol().get());
		getIdpaLinks().setApplicationDeltaLink(
				RestApi.Idpa.Application.GET_DELTA.requestUrl(aid.toString()).withHost(applicationName).withQuery("since", before.toString()).withoutProtocol().get());
		setAppId(aid);
	}

}
