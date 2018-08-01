package org.continuity.api.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class RestEndpointTest {

	@Test
	public void testParameterParsing() {
		assertThat(RestApi.JMeter.TestPlan.GET.parsePathParameters("jmeter/loadtest/test-id")).isEqualTo(Collections.singletonList("test-id"));
		assertThat(RestApi.JMeter.TestPlan.GET.parsePathParameters("jmeter/load/test-id")).isNull();
		assertThat(RestApi.JMeter.TestPlan.GET.parsePathParameters("jmeter/loadtest/test-id/bar")).isNull();

		assertThat(RestApi.SessionLogs.GET.parsePathParameters("myhost/sessions/my-id-123")).isEqualTo(Collections.singletonList("my-id-123"));

		assertThat(RestApi.IdpaApplication.OpenApi.UPDATE_FROM_URL.parsePathParameters("application/openapi/mytag/2.0/url")).isEqualTo(Arrays.asList("mytag", "2.0"));
	}

}
