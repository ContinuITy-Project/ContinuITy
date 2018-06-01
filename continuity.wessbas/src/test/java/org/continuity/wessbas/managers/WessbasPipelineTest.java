package org.continuity.wessbas.managers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Consumer;

import org.continuity.api.entities.config.WorkloadModelReservedConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;

public class WessbasPipelineTest {

	// private AmqpTemplate amqpMock;

	private RestTemplate restMock;

	private Consumer<WorkloadModel> consumerMock;

	WorkloadModelReservedConfig mData;

	WessbasPipelineManager pipelineManager;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws UnsupportedEncodingException {
		mData = new WorkloadModelReservedConfig();
		mData.setStorageLink("wessbas/model/test-1");
		mData.setDataLink("some/link");
		String urlString = "http://session-logs/?link=" + URLEncoder.encode(mData.getDataLink(), "UTF-8") + "&tag=test";
		restMock = Mockito.mock(RestTemplate.class);
		String result = "DAC0E7CAC657D59A1328DEAC1F1F9472;\"ShopGET\":1511777946984000000:1511777947595000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:conversationId=1:<no-encoding>;\"HomeGET\":1511777963338000000:1511777963415000000:/dvdstore/home:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>;\"ShopGET\":1511779159657000000:1511779159856000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>";
		Mockito.when(restMock.getForObject(urlString, String.class)).thenReturn(result);

		consumerMock = Mockito.mock(Consumer.class);
		pipelineManager = new WessbasPipelineManager(consumerMock, restMock);
	}

	@Test
	public void test() {
		pipelineManager.runPipeline(mData);

		ArgumentCaptor<WorkloadModel> workloadModelCaptor = ArgumentCaptor.forClass(WorkloadModel.class);
		Mockito.verify(consumerMock).accept(workloadModelCaptor.capture());
		WorkloadModel workloadModel = workloadModelCaptor.getValue();

		assertThat(workloadModel.getApplicationModel().getSessionLayerEFSM().getApplicationStates()).extracting(state -> state.getService().getName()).containsExactlyInAnyOrder("INITIAL", "HomeGET",
				"ShopGET");
	}
}
