package org.continuity.idpa.annotation.validation;

import org.continuity.api.entities.report.AnnotationViolation;
import org.continuity.api.entities.report.AnnotationViolationType;
import org.continuity.api.entities.report.ApplicationChange;
import org.continuity.api.entities.report.ApplicationChangeType;
import org.continuity.api.entities.report.ModelElementReference;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Henning Schulz
 *
 */
public class SimpleValidityReportTest {

	@Test
	public void test() {
		AnnotationValidityReportBuilder builder = new AnnotationValidityReportBuilder();
		Assert.assertTrue(builder.buildReport().isOk());
		Assert.assertFalse(builder.buildReport().isBreaking());

		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.PARAMETER_ADDED, new ModelElementReference("foo", "bar")));
		Assert.assertFalse(builder.buildReport().isOk());
		Assert.assertFalse(builder.buildReport().isBreaking());

		builder.addApplicationChange(new ApplicationChange(ApplicationChangeType.ENDPOINT_CHANGED, new ModelElementReference("123", "42")));
		Assert.assertFalse(builder.buildReport().isOk());
		Assert.assertFalse(builder.buildReport().isBreaking());

		builder.addViolation(new AnnotationViolation(AnnotationViolationType.ILLEGAL_ENDPOINT_REFERENCE, new ModelElementReference("xyz", "abc")));
		Assert.assertFalse(builder.buildReport().isOk());
		Assert.assertTrue(builder.buildReport().isBreaking());
	}

}
