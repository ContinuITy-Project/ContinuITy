package org.continuity.wessbas.entities;

import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.api.rest.RestApi.Wessbas;

/**
 * @author Henning Schulz
 *
 */
public class WorkloadModelPack extends LinkExchangeModel {

	private static final String DEFAULT_MODEL_TYPE = "wessbas";

	private static final String ERROR_LINK = "INVALID";

	public WorkloadModelPack(String hostname, String id, String tag) {
		setWorkloadType(DEFAULT_MODEL_TYPE);
		setWorkloadLink(hostname + Wessbas.Model.GET_WORKLOAD.path(id));
		setApplicationLink(hostname + Wessbas.Model.GET_APPLICATION.path(id));
		setInitialAnnotationLink(hostname + Wessbas.Model.GET_ANNOTATION.path(id));
		setJmeterLink(hostname + Wessbas.JMeter.CREATE.path(id));

		setTag(tag);
		setError(false);
	}

	public static WorkloadModelPack asError(String hostname, String id, String tag) {
		WorkloadModelPack pack = new WorkloadModelPack(hostname, id, tag);
		pack.setApplicationLink(ERROR_LINK);
		pack.setInitialAnnotationLink(ERROR_LINK);
		pack.setJmeterLink(ERROR_LINK);
		pack.setError(true);

		return pack;
	}

}
