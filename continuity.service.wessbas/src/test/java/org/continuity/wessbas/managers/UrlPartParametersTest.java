package org.continuity.wessbas.managers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.visitor.FindBy;
import org.continuity.wessbas.entities.WessbasBundle;
import org.continuity.wessbas.transform.annotation.AnnotationFromWessbasExtractor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

public class UrlPartParametersTest {

	private static final SessionLogs SESSION_LOG = new SessionLogs(new Date(),
			"123;\"fooRequest\":1:42:/foo/{bar}:8080:localhost:HTTP/1.1:GET:abc=123&URL_PART_bar=hi:<no-encoding>;\"fooRequest\":43:50:/foo/{bar}:8080:localhost:HTTP/1.1:GET:URL_PART_bar=hello:<no-encoding>");

	private RestTemplate restMock;

	private WessbasPipelineManager manager;

	@Before
	public void setup() {
		restMock = Mockito.mock(RestTemplate.class);
		Mockito.when(restMock.getForObject(Mockito.anyString(), Mockito.any())).thenReturn(SESSION_LOG);

		manager = new WessbasPipelineManager(restMock);
	}

	@Test
	public void testTransformationFromSessionLog() {
		TaskDescription task = new TaskDescription();
		LinkExchangeModel  source = new LinkExchangeModel();
		source.getSessionLogsLinks().setLink("");
		task.setSource(source);
		WessbasBundle bundle = manager.runPipeline(task, null);

		AnnotationFromWessbasExtractor extractor = new AnnotationFromWessbasExtractor(bundle.getWorkloadModel());
		Application systemModel = extractor.extractSystemModel();
		ApplicationAnnotation annotation = extractor.extractInitialAnnotation();

		assertThat(systemModel.getEndpoints()).extracting(interf -> (HttpEndpoint) interf).extracting(HttpEndpoint::getPath).containsExactly("/foo/{bar}");

		assertThat(systemModel.getEndpoints()).extracting(interf -> (HttpEndpoint) interf).flatExtracting(HttpEndpoint::getParameters).extracting(HttpParameter::getName)
				.containsExactlyInAnyOrder("bar", "abc");

		assertThat(systemModel.getEndpoints()).extracting(interf -> (HttpEndpoint) interf).flatExtracting(HttpEndpoint::getParameters).extracting(HttpParameter::getParameterType)
		.containsExactlyInAnyOrder(HttpParameterType.URL_PART, HttpParameterType.REQ_PARAM);

		DirectListInput input = FindBy.findById("Input_fooRequest_bar_URL_PART", DirectListInput.class).in(annotation).getFound();

		assertThat(input.getData()).containsExactlyInAnyOrder("hi", "hello");
	}

}
