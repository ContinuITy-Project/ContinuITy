package org.continuity.wessbas.entities;

import org.continuity.api.entities.config.WorkloadModelType;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi.Wessbas;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelPack extends LinkExchangeModel {

	public WorkloadModelPack(String hostname, String id, String tag) {
		getWorkloadModelLinks().setType(WorkloadModelType.WESSBAS).setLink(hostname + Wessbas.Model.OVERVIEW.path(id)).setApplicationLink(hostname + Wessbas.Model.GET_APPLICATION.path(id))
				.setInitialAnnotationLink(hostname + Wessbas.Model.GET_ANNOTATION.path(id)).setJmeterLink(hostname + Wessbas.JMeter.CREATE.path(id)).setBehaviorLink(hostname + Wessbas.BehaviorModel.CREATE.path(id));

		setTag(tag);
	}

}
