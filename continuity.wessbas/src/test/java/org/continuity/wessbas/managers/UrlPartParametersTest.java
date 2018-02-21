package org.continuity.wessbas.managers;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.annotation.dsl.ann.DirectDataInput;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.HttpParameterType;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.visitor.FindById;
import org.continuity.wessbas.entities.MonitoringData;
import org.continuity.wessbas.transform.annotation.AnnotationFromWessbasExtractor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;

public class UrlPartParametersTest {

	private static final String SESSION_LOG = "123;\"fooRequest\":1:42:/foo/{bar}:8080:localhost:HTTP/1.1:GET:abc=123&URL_PART_bar=hi:<no-encoding>;\"fooRequest\":43:50:/foo/{bar}:8080:localhost:HTTP/1.1:GET:URL_PART_bar=hello:<no-encoding>";

	private RestTemplate restMock;

	private WessbasPipelineManager manager;

	private MonitoringData data;

	@Before
	public void setup() {
		restMock = Mockito.mock(RestTemplate.class);
		Mockito.when(restMock.getForObject(Mockito.anyString(), Mockito.any())).thenReturn(SESSION_LOG);

		manager = new WessbasPipelineManager(this::testCreatedWessbasModel, restMock);

		data = new MonitoringData();
		data.setDataLink("http://data-link");
		data.setStorageLink("wessbas/model/mytag-1");
	}

	@Test
	public void testTransformationFromSessionLog() {
		manager.runPipeline(data);
	}

	private void testCreatedWessbasModel(WorkloadModel wessbasModel) {
		AnnotationFromWessbasExtractor extractor = new AnnotationFromWessbasExtractor(wessbasModel);
		SystemModel systemModel = extractor.extractSystemModel();
		SystemAnnotation annotation = extractor.extractInitialAnnotation();

		assertThat(systemModel.getInterfaces()).extracting(interf -> (HttpInterface) interf).extracting(HttpInterface::getPath).containsExactly("/foo/{bar}");

		assertThat(systemModel.getInterfaces()).extracting(interf -> (HttpInterface) interf).flatExtracting(HttpInterface::getParameters).extracting(HttpParameter::getName)
				.containsExactlyInAnyOrder("bar", "abc");

		assertThat(systemModel.getInterfaces()).extracting(interf -> (HttpInterface) interf).flatExtracting(HttpInterface::getParameters).extracting(HttpParameter::getParameterType)
		.containsExactlyInAnyOrder(HttpParameterType.URL_PART, HttpParameterType.REQ_PARAM);

		DirectDataInput input = FindById.find("Input_fooRequest_bar_URL_PART", DirectDataInput.class).in(annotation).getFound();

		assertThat(input.getData()).containsExactlyInAnyOrder("hi", "hello");
	}

}
