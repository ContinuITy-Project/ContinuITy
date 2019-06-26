package org.continuity.wessbas.managers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.continuity.api.entities.artifact.SessionLogs;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.links.LinkExchangeModel;
import org.continuity.wessbas.entities.WessbasBundle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;

public class WessbasPipelineTest {

	private static final SessionLogs SESSION_LOG = new SessionLogs(new Date(),
			"DAC0E7CAC657D59A1328DEAC1F1F9472;\"ShopGET\":1511777946984000000:1511777947595000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:conversationId=1:<no-encoding>;\"HomeGET\":1511777963338000000:1511777963415000000:/dvdstore/home:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>;\"ShopGET\":1511779159657000000:1511779159856000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>");

	private RestTemplate restMock;

	WessbasPipelineManager pipelineManager;

	@Before
	public void setup() throws UnsupportedEncodingException {
		restMock = Mockito.mock(RestTemplate.class);
		Mockito.when(restMock.getForObject(Mockito.anyString(), Mockito.any())).thenReturn(SESSION_LOG);

		pipelineManager = new WessbasPipelineManager(restMock);
	}

	@Test
	public void test() {
		TaskDescription task = new TaskDescription();
		LinkExchangeModel  source = new LinkExchangeModel();
		source.getSessionLogsLinks().setLink("");
		task.setSource(source);
		WessbasBundle bundle = pipelineManager.runPipeline(task, null);

		WorkloadModel workloadModel = bundle.getWorkloadModel();

		assertThat(workloadModel.getApplicationModel().getSessionLayerEFSM().getApplicationStates()).extracting(state -> state.getService().getName()).containsExactlyInAnyOrder("INITIAL", "HomeGET",
				"ShopGET");
	}
}
