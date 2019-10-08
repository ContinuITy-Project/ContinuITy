package org.continuity.request.rates.entities;

import org.continuity.api.entities.config.WorkloadModelType;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi.RequestRates;
import org.continuity.idpa.AppId;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelPack extends LinkExchangeModel {

	public WorkloadModelPack(String hostname, String id, AppId aid) {
		getWorkloadModelLinks().setType(WorkloadModelType.REQUEST_RATES).setLink(hostname + RequestRates.Model.OVERVIEW.path(id))
				.setApplicationLink(hostname + RequestRates.Model.GET_APPLICATION.path(id)).setInitialAnnotationLink(hostname + RequestRates.Model.GET_ANNOTATION.path(id))
				.setJmeterLink(hostname + RequestRates.JMeter.CREATE.path(id));

		setAppId(aid);
	}

}
