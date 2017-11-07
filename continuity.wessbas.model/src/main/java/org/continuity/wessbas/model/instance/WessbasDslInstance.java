package org.continuity.wessbas.model.instance;

import java.io.IOException;

import org.continuity.commons.wessbas.WessbasModelParser;

import m4jdsl.ApplicationModel;
import m4jdsl.ApplicationState;
import m4jdsl.M4jdslFactory;
import m4jdsl.Parameter;
import m4jdsl.Property;
import m4jdsl.ProtocolLayerEFSM;
import m4jdsl.ProtocolState;
import m4jdsl.Request;
import m4jdsl.Service;
import m4jdsl.SessionLayerEFSM;
import m4jdsl.WorkloadModel;

/**
 * @author Henning Schulz
 *
 */
public enum WessbasDslInstance {

	SIMPLE {
		@Override
		public WorkloadModel get() {
			WorkloadModel createdWessbasModel = M4jdslFactory.eINSTANCE.createWorkloadModel();
			ApplicationModel applicationModel = M4jdslFactory.eINSTANCE.createApplicationModel();
			SessionLayerEFSM sessionEfsm = M4jdslFactory.eINSTANCE.createSessionLayerEFSM();
			createdWessbasModel.setApplicationModel(applicationModel);
			applicationModel.setSessionLayerEFSM(sessionEfsm);

			sessionEfsm.getApplicationStates().add(createApplicationState("Login", "localhost", "80", "/login", "GET", "<no-encoding>", "https", "user", "password"));
			sessionEfsm.getApplicationStates().add(createApplicationState("Add2Cart", "localhost", "80", "/cart/{id}/add", "POST", "<no-encoding>", "https", "URL_PART:id"));
			sessionEfsm.getApplicationStates().add(createApplicationState("Purchase", "localhost", "80", "/purchase", "GET", "<no-encoding>", "https", "BODY"));

			return createdWessbasModel;
		}
	},

	SPECJ_PARSED {
		@Override
		public WorkloadModel get() {
			WessbasModelParser parser = new WessbasModelParser();
			String path = getClass().getResource("/workloadmodel-specj.xmi").getPath();
			try {
				return parser.readWorkloadModel(path);
			} catch (IOException e) {
				throw new RuntimeException("Error when reading " + path + "!", e);
			}
		}
	},

	DVDSTORE_PARSED {
		@Override
		public WorkloadModel get() {
			WessbasModelParser parser = new WessbasModelParser();
			String path = getClass().getResource("/workloadmodel-dvdstore.xmi").getPath();
			try {
				return parser.readWorkloadModel(path);
			} catch (IOException e) {
				throw new RuntimeException("Error when reading " + path + "!", e);
			}
		}
	};

	public abstract WorkloadModel get();

	private static ApplicationState createApplicationState(String name, String domain, String port, String path, String method, String encoding, String protocol, String... parameters) {
		ApplicationState appState = M4jdslFactory.eINSTANCE.createApplicationState();
		Service service = M4jdslFactory.eINSTANCE.createService();
		ProtocolLayerEFSM protocolEfsm = M4jdslFactory.eINSTANCE.createProtocolLayerEFSM();
		ProtocolState protocolState = M4jdslFactory.eINSTANCE.createProtocolState();
		Request request = M4jdslFactory.eINSTANCE.createHTTPRequest();
		appState.setProtocolDetails(protocolEfsm);
		appState.setService(service);
		service.setName(name);
		protocolEfsm.getProtocolStates().add(protocolState);
		protocolEfsm.setInitialState(protocolState);
		protocolState.setRequest(request);

		request.getProperties().add(createHttpProperty("domain", domain));
		request.getProperties().add(createHttpProperty("port", port));
		request.getProperties().add(createHttpProperty("path", path));
		request.getProperties().add(createHttpProperty("method", method));
		request.getProperties().add(createHttpProperty("encoding", encoding));
		request.getProperties().add(createHttpProperty("protocol", protocol));

		for (String param : parameters) {
			Parameter p = M4jdslFactory.eINSTANCE.createParameter();
			p.setName(param);
			request.getParameters().add(p);
		}

		return appState;
	}

	private static Property createHttpProperty(String name, String value) {
		Property property = M4jdslFactory.eINSTANCE.createProperty();
		property.setKey("HTTPSampler." + name);
		property.setValue(value);
		return property;
	}

}
