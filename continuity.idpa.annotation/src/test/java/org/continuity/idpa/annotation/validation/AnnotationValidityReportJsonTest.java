package org.continuity.idpa.annotation.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.continuity.api.entities.report.AnnotationValidityReport;
import org.continuity.api.entities.report.AnnotationViolation;
import org.continuity.api.entities.report.AnnotationViolationType;
import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.entities.report.ModelElementReference;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationValidityReportJsonTest {

	private AnnotationValidityReport report;

	private ObjectNode fooReference;
	private ObjectNode barReference;
	private ObjectNode blubReference;

	@Before
	public void setupReport() {
		Map<ModelElementReference, Set<AnnotationViolation>> violations = new HashMap<>();
		violations.put(new ModelElementReference("MyType", "MyId"),
				Collections.singleton(new AnnotationViolation(AnnotationViolationType.ILLEGAL_ENDPOINT_REFERENCE, new ModelElementReference("HttpEndpoint", "foo"))));

		Set<ApplicationChange> systemChanges = new HashSet<>(Arrays.asList(new ApplicationChange(ApplicationChangeType.ENDPOINT_REMOVED, new ModelElementReference("HttpEndpoint", "bar")),
				new ApplicationChange(ApplicationChangeType.PARAMETER_ADDED, new ModelElementReference("HttpParameter", "blub"))));

		report = new AnnotationValidityReport(systemChanges, violations);
	}

	@Before
	public void setupReference() {
		ObjectMapper mapper = new ObjectMapper();

		fooReference = mapper.createObjectNode();
		fooReference.putObject("affected-element").put("type", "HttpEndpoint").put("id", "foo");
		fooReference.put("breaking", true).put("message", AnnotationViolationType.ILLEGAL_ENDPOINT_REFERENCE.getMessage());

		barReference = mapper.createObjectNode();
		barReference.putObject("changed-element").put("type", "HttpEndpoint").put("id", "bar");
		barReference.put("message", ApplicationChangeType.ENDPOINT_REMOVED.getMessage());

		blubReference = mapper.createObjectNode();
		blubReference.putObject("changed-element").put("type", "HttpParameter").put("id", "blub");
		blubReference.put("message", ApplicationChangeType.PARAMETER_ADDED.getMessage());

	}

	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode parsed = mapper.readTree(report.toString());
		assertThat(parsed.path("violations").path(new ModelElementReference("MyType", "MyId").toString())).containsExactlyInAnyOrder(fooReference);
		assertThat(parsed.path("application-changes")).containsExactlyInAnyOrder(barReference, blubReference);
	}

}
