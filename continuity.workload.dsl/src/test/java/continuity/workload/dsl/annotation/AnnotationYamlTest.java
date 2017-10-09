package continuity.workload.dsl.annotation;

import java.io.IOException;

import org.continuity.workload.dsl.WeakReference;
import org.continuity.workload.dsl.annotation.CsvInput;
import org.continuity.workload.dsl.annotation.DirectDataInput;
import org.continuity.workload.dsl.annotation.ExtractedInput;
import org.continuity.workload.dsl.annotation.InterfaceAnnotation;
import org.continuity.workload.dsl.annotation.ParameterAnnotation;
import org.continuity.workload.dsl.annotation.RegExExtraction;
import org.continuity.workload.dsl.annotation.SystemAnnotation;
import org.continuity.workload.dsl.annotation.UnknownDataInput;
import org.continuity.workload.dsl.annotation.yaml.AnnotationYamlSerializer;
import org.continuity.workload.dsl.system.HttpInterface;
import org.continuity.workload.dsl.system.HttpParameter;
import org.continuity.workload.dsl.system.HttpParameterType;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Henning Schulz
 *
 */
public class AnnotationYamlTest {

	private static SystemAnnotation annotation;

	@BeforeClass
	public static void setupAnnotation() {
		// System model
		HttpInterface interf = new HttpInterface();
		interf.setId("login");

		HttpParameter param = new HttpParameter();
		param.setId("user");
		param.setParameterType(HttpParameterType.REQ_PARAM);

		interf.getParameters().add(param);

		// Input

		DirectDataInput input = new DirectDataInput();
		input.setId("DAT1");
		input.getData().add("foo");
		input.getData().add("bar");

		CsvInput csvInput = new CsvInput();
		csvInput.setFilename("myfile.csv");
		csvInput.setColumn(3);
		csvInput.getAssociated().add(input);

		ExtractedInput extrInput = new ExtractedInput();
		RegExExtraction extr = new RegExExtraction();
		extr.setExtracted(WeakReference.create(interf));
		extr.setPattern("(.*)");
		extrInput.getExtractions().add(extr);

		UnknownDataInput unknownInput = new UnknownDataInput();
		unknownInput.setId("UNK1");
		unknownInput.setType("MyCustomDataInput");

		// Annotation

		annotation = new SystemAnnotation();
		annotation.getInputs().add(input);
		annotation.getInputs().add(csvInput);
		annotation.getInputs().add(extrInput);
		annotation.getInputs().add(unknownInput);
		annotation.setId("ANN");

		InterfaceAnnotation interfaceAnn = new InterfaceAnnotation();
		interfaceAnn.setAnnotatedInterface(WeakReference.create(interf));

		ParameterAnnotation paramAnn = new ParameterAnnotation();
		paramAnn.setAnnotatedParameter(WeakReference.create(param));
		paramAnn.setInput(input);

		interfaceAnn.getParameterAnnotations().add(paramAnn);
		annotation.getInterfaceAnnotations().add(interfaceAnn);
	}

	@Test
	public void test() throws JsonGenerationException, JsonMappingException, IOException {
		AnnotationYamlSerializer serializer = new AnnotationYamlSerializer();
		serializer.writeToYaml(annotation, "annotation.yml");
		serializer.readFromYaml("annotation.yml");
	}

}
