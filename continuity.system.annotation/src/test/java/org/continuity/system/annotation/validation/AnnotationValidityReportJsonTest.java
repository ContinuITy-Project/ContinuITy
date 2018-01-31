package org.continuity.system.annotation.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.continuity.system.annotation.entities.AnnotationValidityReport;
import org.continuity.system.annotation.entities.AnnotationViolation;
import org.continuity.system.annotation.entities.AnnotationViolationType;
import org.continuity.system.annotation.entities.ModelElementReference;
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
				Collections.singleton(new AnnotationViolation(AnnotationViolationType.INTERFACE_ADDED, new ModelElementReference("HttpInterface", "foo"))));

		Set<AnnotationViolation> systemChanges = new HashSet<>(Arrays.asList(new AnnotationViolation(AnnotationViolationType.INTERFACE_REMOVED, new ModelElementReference("HttpInterface", "bar")),
				new AnnotationViolation(AnnotationViolationType.PARAMETER_ADDED, new ModelElementReference("HttpParameter", "blub"))));

		report = new AnnotationValidityReport(systemChanges, violations);
	}

	@Before
	public void setupReference() {
		ObjectMapper mapper = new ObjectMapper();

		fooReference = mapper.createObjectNode();
		fooReference.putObject("changed-element").put("type", "HttpInterface").put("id", "foo");
		fooReference.put("breaking", false).put("message", AnnotationViolationType.INTERFACE_ADDED.getMessage());

		barReference = mapper.createObjectNode();
		barReference.putObject("changed-element").put("type", "HttpInterface").put("id", "bar");
		barReference.put("breaking", false).put("message", AnnotationViolationType.INTERFACE_REMOVED.getMessage());

		blubReference = mapper.createObjectNode();
		blubReference.putObject("changed-element").put("type", "HttpParameter").put("id", "blub");
		blubReference.put("breaking", false).put("message", AnnotationViolationType.PARAMETER_ADDED.getMessage());
	}

	@Test
	public void test() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode parsed = mapper.readTree(report.toString());
		assertThat(parsed.path("violations").path(new ModelElementReference("MyType", "MyId").toString())).containsExactlyInAnyOrder(fooReference);
		assertThat(parsed.path("system-changes")).containsExactlyInAnyOrder(barReference, blubReference);
	}

}
