package org.continuity.wessbas.entities;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.entities.order.WorkloadModelType;
import org.continuity.api.rest.RestApi;
import org.continuity.api.rest.RestApi.Wessbas;
import org.continuity.idpa.AppId;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelPack extends LinkExchangeModel {

	public WorkloadModelPack(String hostname, String id, AppId aid) {
			getWorkloadModelLinks().setType(WorkloadModelType.WESSBAS).setLink(hostname + Wessbas.Model.OVERVIEW.path(id)).setApplicationLink(hostname + Wessbas.Model.GET_APPLICATION.path(id))
					.setInitialAnnotationLink(hostname + Wessbas.Model.GET_ANNOTATION.path(id)).setJmeterLink(hostname + Wessbas.JMeter.CREATE.path(id))
					.setBehaviorLink(hostname + Wessbas.BehaviorModel.CREATE.path(id));
		setAppId(aid);
	}

	public WorkloadModelPack(String hostname, String id, AppId aid, boolean isModularized) {
		if (!isModularized) {
			getWorkloadModelLinks().setType(WorkloadModelType.WESSBAS).setLink(hostname + Wessbas.Model.OVERVIEW.path(id)).setApplicationLink(hostname + Wessbas.Model.GET_APPLICATION.path(id))
					.setInitialAnnotationLink(hostname + Wessbas.Model.GET_ANNOTATION.path(id)).setJmeterLink(hostname + Wessbas.JMeter.CREATE.path(id))
					.setBehaviorLink(hostname + Wessbas.BehaviorModel.CREATE.path(id));
		} else {
			getWorkloadModelLinks().setType(WorkloadModelType.WESSBAS).setLink(hostname + Wessbas.Model.OVERVIEW.path(id)).setApplicationLink(RestApi.Idpa.Application.GET.requestUrl(aid).get())
			.setInitialAnnotationLink(hostname + Wessbas.Model.GET_ANNOTATION.path(id)).setJmeterLink(hostname + Wessbas.JMeter.CREATE.path(id))
			.setBehaviorLink(hostname + Wessbas.BehaviorModel.CREATE.path(id));
		}
		setAppId(aid);
	}

}
