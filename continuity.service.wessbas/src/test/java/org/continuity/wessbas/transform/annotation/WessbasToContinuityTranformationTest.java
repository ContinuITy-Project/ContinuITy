package org.continuity.wessbas.transform.annotation;

import java.io.IOException;

import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.Application;
import org.continuity.wessbas.entities.WessbasDslInstance;
import org.continuity.wessbas.transform.annotation.AnnotationFromWessbasExtractor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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
		parsedWessbasModel = WessbasDslInstance.SPECJ_PARSED.get();
	}

	@BeforeClass
	public static void setupWessbasModel() {
		createdWessbasModel = WessbasDslInstance.SIMPLE.get();
	}

	@Test
	public void testCreatedModel() {
		AnnotationFromWessbasExtractor extractor = new AnnotationFromWessbasExtractor(createdWessbasModel, "test");
		Application system = extractor.extractSystemModel();

		Assert.assertEquals("Expected the system to have 3 interfaces.", 3, system.getEndpoints().size());

		for (Endpoint<?> interf : system.getEndpoints()) {
			switch (interf.getId()) {
			case "Login":
				Assert.assertEquals("Wrong method for Login!", "GET", ((HttpEndpoint) interf).getMethod());
				break;
			case "Add2Cart":
				Assert.assertEquals("Wrong method for Add2Cart!", "POST", ((HttpEndpoint) interf).getMethod());
				break;
			case "Purchase":
				Assert.assertEquals("Wrong method for Purchase!", "GET", ((HttpEndpoint) interf).getMethod());
				break;
			default:
				Assert.fail("Found the unexpected interface " + interf.getId());
				break;
			}
		}

		ApplicationAnnotation annotation = extractor.extractInitialAnnotation();
		Assert.assertEquals("Extracted system and annotation should have the same number of interfaces!", system.getEndpoints().size(), annotation.getEndpointAnnotations().size());
	}

	@Test
	public void testParsedModel() {
		AnnotationFromWessbasExtractor extractor = new AnnotationFromWessbasExtractor(parsedWessbasModel, "specj");
		Application system = extractor.extractSystemModel();

		Assert.assertEquals("Expected the system to have 13 interfaces.", 13, system.getEndpoints().size());
	}

}
