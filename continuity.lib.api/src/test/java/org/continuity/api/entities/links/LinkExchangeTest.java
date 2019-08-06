package org.continuity.api.entities.links;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.continuity.api.entities.exchange.ArtifactExchangeModel;
import org.continuity.api.entities.exchange.WorkloadModelType;
import org.continuity.api.entities.order.LoadTestType;
import org.continuity.idpa.AppId;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LinkExchangeTest {

	private ArtifactExchangeModel model;

	private ArtifactExchangeModel secondModel;

	private ObjectMapper mapper = new ObjectMapper();

	@Before
	public void setup() {
		model = new ArtifactExchangeModel().setAppId(AppId.fromString("foo")).getWorkloadModelLinks().setApplicationLink("foo/bar/app").setType(WorkloadModelType.WESSBAS).parent();

		secondModel = new ArtifactExchangeModel().setAppId(AppId.fromString("bar")).getLoadTestLinks().setLink("abc/xyz/loadtest").setType(LoadTestType.JMETER).parent();
	}

	@Test
	public void testForEqualJsons() throws IOException {
		String json1 = mapper.writeValueAsString(model);
		ArtifactExchangeModel read = mapper.readValue(json1, ArtifactExchangeModel.class);
		String json2 = mapper.writeValueAsString(read);

		assertThat(json2).isEqualTo(json1).as("The re-generated json should be equal to the original one!");
	}

	@Test
	public void testForParent() throws IOException {
		String json1 = mapper.writeValueAsString(model);
		ArtifactExchangeModel read = mapper.readValue(json1, ArtifactExchangeModel.class);

		assertThat(read.getTraceLinks()).isNotNull();
		assertThat(read.getLoadTestLinks()).isNotNull();
		assertThat(read.getSessionLinks()).isNotNull();
		assertThat(read.getWorkloadModelLinks()).isNotNull();

		assertThat(read.getTraceLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
		assertThat(read.getLoadTestLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
		assertThat(read.getSessionLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
		assertThat(read.getWorkloadModelLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
	}

	@Test
	public void testMerge() {
		model.merge(secondModel);

		assertThat(model.getAppId()).isEqualTo(AppId.fromString("foo"));
		assertThat(model.getWorkloadModelLinks().getApplicationLink()).isEqualTo("foo/bar/app");
		assertThat(model.getWorkloadModelLinks().getType()).isEqualTo(WorkloadModelType.WESSBAS);
		assertThat(model.getLoadTestLinks().getLink()).isEqualTo("abc/xyz/loadtest");
		assertThat(model.getLoadTestLinks().getType()).isEqualTo(LoadTestType.JMETER);
	}

}
