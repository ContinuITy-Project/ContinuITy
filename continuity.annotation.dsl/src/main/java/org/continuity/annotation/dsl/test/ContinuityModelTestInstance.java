package org.continuity.annotation.dsl.test;

import java.io.IOException;

import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.ann.CsvInput;
import org.continuity.annotation.dsl.ann.CustomDataInput;
import org.continuity.annotation.dsl.ann.DirectDataInput;
import org.continuity.annotation.dsl.ann.ExtractedInput;
import org.continuity.annotation.dsl.ann.Input;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.ann.PropertyOverride;
import org.continuity.annotation.dsl.ann.PropertyOverrideKey;
import org.continuity.annotation.dsl.ann.RegExExtraction;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.custom.CustomAnnotation;
import org.continuity.annotation.dsl.custom.CustomAnnotationElement;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.HttpParameterType;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.yaml.ContinuityYamlSerializer;

/**
 * @author Henning Schulz
 *
 */
public enum ContinuityModelTestInstance {

	SIMPLE {

		@Override
		protected SystemModel setupSystemModel() {
			SystemModel system = new SystemModel();

			HttpInterface interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("user");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			system.addInterface(interf);

			return system;
		}

		@Override
		protected SystemAnnotation setupAnnotation(SystemModel system) {
			SystemAnnotation annotation = new SystemAnnotation();

			HttpInterface interf = (HttpInterface) system.getInterfaces().get(0);

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
			extr.setFrom(WeakReference.create(interf));
			extr.setPattern("(.*)");
			extrInput.getExtractions().add(extr);

			CustomDataInput unknownInput = new CustomDataInput();
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
			PropertyOverride<PropertyOverrideKey.InterfaceLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpInterface.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);

			ParameterAnnotation paramAnn = new ParameterAnnotation();
			paramAnn.setAnnotatedParameter(WeakReference.create(interf.getParameters().get(0)));
			paramAnn.setInput(input);

			interfaceAnn.getParameterAnnotations().add(paramAnn);
			annotation.getInterfaceAnnotations().add(interfaceAnn);

			return annotation;
		}

		@Override
		protected CustomAnnotation setupAnnotationExtension(SystemModel system, SystemAnnotation annotation) {
			CustomAnnotation extension = new CustomAnnotation();
			CustomAnnotationElement ext = new CustomAnnotationElement();
			extension.addElement(ext);

			Input unknownInput = annotation.getInputs().get(3);
			ext.setReference(WeakReference.create(unknownInput));
			ext.addProperty("driver", "freak-load-driver");
			ext.addProperty("foo", "bar");

			return extension;
		}

	},

	DVDSTORE_PARSED {
		@Override
		protected SystemModel setupSystemModel() {
			ContinuityYamlSerializer<SystemModel> serializer = new ContinuityYamlSerializer<>(SystemModel.class);

			try {
				return serializer.readFromYamlInputStream(getClass().getResourceAsStream("/dvdstore-systemmodel.yml"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected SystemAnnotation setupAnnotation(SystemModel system) {
			ContinuityYamlSerializer<SystemAnnotation> serializer = new ContinuityYamlSerializer<>(SystemAnnotation.class);
			try {
				return serializer.readFromYamlInputStream(getClass().getResourceAsStream("/dvdstore-annotation.yml"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected CustomAnnotation setupAnnotationExtension(SystemModel system, SystemAnnotation annotation) {
			return null;
		}
	};

	private final SystemModel systemModel;
	private final SystemAnnotation annotation;
	private final CustomAnnotation annotationExtension;

	/**
	 *
	 */
	private ContinuityModelTestInstance() {
		this.systemModel = setupSystemModel();
		this.annotation = setupAnnotation(systemModel);
		this.annotationExtension = setupAnnotationExtension(systemModel, annotation);
	}

	protected abstract SystemModel setupSystemModel();

	protected abstract SystemAnnotation setupAnnotation(SystemModel system);

	protected abstract CustomAnnotation setupAnnotationExtension(SystemModel system, SystemAnnotation annotation);

	/**
	 * Gets {@link #systemModel}.
	 *
	 * @return {@link #systemModel}
	 */
	public SystemModel getSystemModel() {
		return this.systemModel;
	}

	/**
	 * Gets {@link #annotation}.
	 *
	 * @return {@link #annotation}
	 */
	public SystemAnnotation getAnnotation() {
		return this.annotation;
	}

	/**
	 * Gets {@link #annotationExtension}.
	 *
	 * @return {@link #annotationExtension}
	 */
	public CustomAnnotation getAnnotationExtension() {
		return this.annotationExtension;
	}

}
