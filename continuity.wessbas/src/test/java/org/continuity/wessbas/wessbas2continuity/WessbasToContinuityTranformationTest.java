package org.continuity.wessbas.wessbas2continuity;

import java.io.IOException;

import org.continuity.wessbas.utils.WessbasModelParser;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.system.HttpInterface;
import org.continuity.workload.dsl.system.ServiceInterface;
import org.continuity.workload.dsl.system.TargetSystem;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
public class WessbasToContinuityTranformationTest {

	private static WorkloadModel createdWessbasModel;
	private static WorkloadModel parsedWessbasModel;

	@BeforeClass
	public static void loadWessbasModel() throws IOException {
		WessbasModelParser parser = new WessbasModelParser();
		parsedWessbasModel = parser.readWorkloadModel("src/test/resources/workloadmodel-specj.xmi");
	}

	@BeforeClass
	public static void setupWessbasModel() {
		createdWessbasModel = M4jdslFactory.eINSTANCE.createWorkloadModel();
		ApplicationModel applicationModel = M4jdslFactory.eINSTANCE.createApplicationModel();
		SessionLayerEFSM sessionEfsm = M4jdslFactory.eINSTANCE.createSessionLayerEFSM();
		createdWessbasModel.setApplicationModel(applicationModel);
		applicationModel.setSessionLayerEFSM(sessionEfsm);

		sessionEfsm.getApplicationStates().add(createApplicationState("Login", "localhost", "80", "/login", "GET", "<no-encoding>", "https", "user", "password"));
		sessionEfsm.getApplicationStates().add(createApplicationState("Add2Cart", "localhost", "80", "/cart/{id}/add", "POST", "<no-encoding>", "https", "URL_PART:id"));
		sessionEfsm.getApplicationStates().add(createApplicationState("Purchase", "localhost", "80", "/purchase", "GET", "<no-encoding>", "https", "BODY"));
	}

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

	@Test
	public void testCreatedModel() {
		DslFromWessbasExtractor extractor = new DslFromWessbasExtractor(createdWessbasModel, "test");
		TargetSystem system = extractor.extractSystemModel();

		Assert.assertEquals("Expected the system to have 3 interfaces.", 3, system.getInterfaces().size());

		for (ServiceInterface<?> interf : system.getInterfaces()) {
			switch (interf.getId()) {
			case "Login":
				Assert.assertEquals("Wrong method for Login!", "GET", ((HttpInterface) interf).getMethod());
				break;
			case "Add2Cart":
				Assert.assertEquals("Wrong method for Add2Cart!", "POST", ((HttpInterface) interf).getMethod());
				break;
			case "Purchase":
				Assert.assertEquals("Wrong method for Purchase!", "GET", ((HttpInterface) interf).getMethod());
				break;
			default:
				Assert.fail("Found the unexpected interface " + interf.getId());
				break;
			}
		}

		SystemAnnotation annotation = extractor.extractInitialAnnotation();
		Assert.assertEquals("Extracted system and annotation should have the same number of interfaces!", system.getInterfaces().size(), annotation.getInterfaceAnnotations().size());
	}

	@Test
	public void testParsedModel() {
		DslFromWessbasExtractor extractor = new DslFromWessbasExtractor(parsedWessbasModel, "specj");
		TargetSystem system = extractor.extractSystemModel();

		Assert.assertEquals("Expected the system to have 14 interfaces.", 14, system.getInterfaces().size());
	}

}
