package org.continuity.wessbas.managers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.continuity.api.entities.artifact.ForecastIntensityRecord;
import org.continuity.api.entities.artifact.markovbehavior.MarkovBehaviorModel;
import org.continuity.api.entities.artifact.markovbehavior.RelativeMarkovChain;
import org.continuity.api.entities.config.TaskDescription;
import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.BehaviorModelType;
import org.continuity.api.entities.test.MarkovChainTestInstance;
import org.continuity.idpa.AppId;
import org.continuity.idpa.VersionOrTimestamp;
import org.continuity.wessbas.entities.BehaviorModelPack;
import org.continuity.wessbas.entities.WessbasBundle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import m4jdsl.WorkloadModel;
import net.sf.markov4jmeter.m4jdslmodelgenerator.GeneratorException;

public class WessbasPipelineTest {

	private static final String SESSION_LOG = "DAC0E7CAC657D59A1328DEAC1F1F9472;\"ShopGET\":1511777946984000000:1511777947595000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:conversationId=1:<no-encoding>;\"HomeGET\":1511777963338000000:1511777963415000000:/dvdstore/home:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>;\"ShopGET\":1511779159657000000:1511779159856000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>";

	private MarkovBehaviorModel behaviorModel;

	private RestTemplate restMock;

	WessbasPipelineManager pipelineManager;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() throws IOException {
		behaviorModel = new MarkovBehaviorModel();
		RelativeMarkovChain chain1 = RelativeMarkovChain.fromCsv(MarkovChainTestInstance.SOCK_SHOP.getCsv());
		chain1.setId("1");
		RelativeMarkovChain chain2 = RelativeMarkovChain.fromCsv(MarkovChainTestInstance.SOCK_SHOP_WO_CART.getCsv());
		chain2.setId("2");
		behaviorModel.setMarkovChains(Arrays.asList(chain1, chain2));

		restMock = Mockito.mock(RestTemplate.class);
		Mockito.when(restMock.getForObject(Mockito.anyString(), Mockito.eq(String.class))).thenReturn(SESSION_LOG);
		Mockito.when(restMock.exchange(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(Class.class))).thenReturn(ResponseEntity.ok(SESSION_LOG));
		Mockito.when(restMock.getForObject(Mockito.anyString(), Mockito.eq(MarkovBehaviorModel.class))).thenReturn(behaviorModel);

		pipelineManager = new WessbasPipelineManager(restMock);
	}

	@Test
	public void testWithWessbas() {
		TaskDescription task = new TaskDescription();
		ArtifactExchangeModel source = new ArtifactExchangeModel();
		source.getSessionLinks().setExtendedLink("");
		task.setSource(source);
		WessbasBundle bundle = pipelineManager.runPipeline(task, 60000000000L);

		WorkloadModel workloadModel = bundle.getWorkloadModel();

		assertThat(workloadModel.getApplicationModel().getSessionLayerEFSM().getApplicationStates()).extracting(state -> state.getService().getName()).containsExactlyInAnyOrder("INITIAL", "HomeGET",
				"ShopGET");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testWithExternal() throws IOException, SecurityException, GeneratorException, NumberFormatException, ParseException {
		testWithExternal(intensityRecord(0, Pair.of("1", 100.0), Pair.of("2", 50.0)));

		testWithExternal(intensityRecord(0, Pair.of("1", 100.0), Pair.of("2", 50.0)), intensityRecord(1000, Pair.of("1", 80.0), Pair.of("2", 70.0)));

		testWithExternal(intensityRecord(0, Pair.of("1", 100.0), Pair.of("2", 50.0)), intensityRecord(1000, Pair.of("1", 80.0), Pair.of("2", 0.0), Pair.of("3", 70.0)));

		testWithExternal(intensityRecord(0, Pair.of("1", 100.0), Pair.of("2", 50.0)), intensityRecord(1000, Pair.of("1", 80.0), Pair.of("2", 30.0), Pair.of("3", 70.0)));

		testWithExternal(intensityRecord(0, Pair.of(ForecastIntensityRecord.KEY_TOTAL, 100.0)), intensityRecord(1000, Pair.of(ForecastIntensityRecord.KEY_TOTAL, 80.0)));

		testWithExternal(intensityRecord(0, Pair.of(ForecastIntensityRecord.KEY_TOTAL, 100.0)));
	}

	private void testWithExternal(ForecastIntensityRecord... records) throws IOException, SecurityException, GeneratorException, NumberFormatException, ParseException {
		pipelineManager = new WessbasPipelineManager(restMock);
		Mockito.when(restMock.getForObject(Mockito.anyString(), Mockito.eq(ForecastIntensityRecord[].class))).thenReturn(records);

		TaskDescription task = new TaskDescription();
		task.setAppId(AppId.fromString("WessbasPipelineTest"));
		task.setVersion(VersionOrTimestamp.fromString("v1"));
		ArtifactExchangeModel source = new ArtifactExchangeModel();
		source.setIntensity("INTENSITY");
		source.getBehaviorModelLinks().setLink("BEHAVIOR");
		source.getBehaviorModelLinks().setType(BehaviorModelType.MARKOV_CHAIN);
		task.setSource(source);

		BehaviorModelPack pack = pipelineManager.createBehaviorModelFromMarkovChains(task);
		WessbasBundle workloadModel = pipelineManager.transformBehaviorModelToWorkloadModelIncludingTailoring(pack, task);

		List<String> states = behaviorModel.getMarkovChains().get(0).getRequestStates();
		states.add("INITIAL");
		assertThat(workloadModel.getWorkloadModel().getApplicationModel().getSessionLayerEFSM().getApplicationStates()).extracting(state -> state.getService().getName())
				.containsOnlyElementsOf(states);

		double totalMaximumIntensity = 0;

		for (ForecastIntensityRecord rec : records) {
			double sum = 0;

			for (Entry<String, Double> entry : rec.getContent().entrySet()) {
				if (!ForecastIntensityRecord.KEY_TIMESTAMP.equals(entry.getKey())) {
					sum += entry.getValue();
				}
			}

			if (sum > totalMaximumIntensity) {
				totalMaximumIntensity = sum;
			}
		}

		assertThat(workloadModel.getWorkloadModel().getWorkloadIntensity().getFormula()).isEqualTo(Integer.toString((int) totalMaximumIntensity));

		if (records.length > 1) {
			assertThat(workloadModel.getIntensityResolution()).isEqualTo(1000);
			assertThat(workloadModel.getIntensities().size()).isEqualTo(records[0].getContent().containsKey(ForecastIntensityRecord.KEY_TOTAL) ? 1 : 2);

			for (Entry<String, String> entry : workloadModel.getIntensities().entrySet()) {
				assertThat(entry.getValue().split("\\,")).containsExactlyElementsOf(Arrays.stream(records).map(ForecastIntensityRecord::getContent).map(c -> c.get(entry.getKey()))
						.map(d -> Math.toIntExact(Math.round(d))).map(i -> Integer.toString(i)).collect(Collectors.toList()));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private ForecastIntensityRecord intensityRecord(long timestamp, Pair<String, Double>... intensities) {
		Map<String, Double> content = new HashMap<>();
		content.put(ForecastIntensityRecord.KEY_TIMESTAMP, (double) timestamp);

		for (Pair<String, Double> in : intensities) {
			content.put(in.getKey(), in.getValue());
		}

		return new ForecastIntensityRecord(content);
	}

}
