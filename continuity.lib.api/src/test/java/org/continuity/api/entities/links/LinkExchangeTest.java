package org.continuity.api.entities.links;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.continuity.api.entities.config.LoadTestType;
import org.continuity.api.entities.config.WorkloadModelType;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class LinkExchangeTest {

	private LinkExchangeModel model;

	private LinkExchangeModel secondModel;

	private ObjectMapper mapper = new ObjectMapper();

	@Before
	public void setup() {
		model = new LinkExchangeModel().setTag("foo").getWorkloadModelLinks().setApplicationLink("foo/bar/app").setType(WorkloadModelType.WESSBAS).parent();

		secondModel = new LinkExchangeModel().setTag("bar").getLoadTestLinks().setLink("abc/xyz/loadtest").setType(LoadTestType.JMETER).parent();
	}

	@Test
	public void testForEqualJsons() throws IOException {
		String json1 = mapper.writeValueAsString(model);
		LinkExchangeModel read = mapper.readValue(json1, LinkExchangeModel.class);
		String json2 = mapper.writeValueAsString(read);

		assertThat(json2).isEqualTo(json1).as("The re-generated json should be equal to the original one!");
	}

	@Test
	public void testForParent() throws IOException {
		String json1 = mapper.writeValueAsString(model);
		LinkExchangeModel read = mapper.readValue(json1, LinkExchangeModel.class);

		assertThat(read.getMeasurementDataLinks()).isNotNull();
		assertThat(read.getIdpaLinks()).isNotNull();
		assertThat(read.getLoadTestLinks()).isNotNull();
		assertThat(read.getSessionLogsLinks()).isNotNull();
		assertThat(read.getWorkloadModelLinks()).isNotNull();

		assertThat(read.getMeasurementDataLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
		assertThat(read.getIdpaLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
		assertThat(read.getLoadTestLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
		assertThat(read.getSessionLogsLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
		assertThat(read.getWorkloadModelLinks().parent()).isEqualTo(read).as("The parent has to be the actual parent!");
	}

	@Test
	public void testMerge() {
		model.merge(secondModel);

		assertThat(model.getTag()).isEqualTo("foo");
		assertThat(model.getWorkloadModelLinks().getApplicationLink()).isEqualTo("foo/bar/app");
		assertThat(model.getWorkloadModelLinks().getType()).isEqualTo(WorkloadModelType.WESSBAS);
		assertThat(model.getLoadTestLinks().getLink()).isEqualTo("abc/xyz/loadtest");
		assertThat(model.getLoadTestLinks().getType()).isEqualTo(LoadTestType.JMETER);
	}

}
