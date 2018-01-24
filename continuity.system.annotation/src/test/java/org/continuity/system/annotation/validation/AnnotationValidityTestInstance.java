package org.continuity.system.annotation.validation;

import org.continuity.annotation.dsl.WeakReference;
import org.continuity.annotation.dsl.ann.CsvInput;
import org.continuity.annotation.dsl.ann.CustomDataInput;
import org.continuity.annotation.dsl.ann.DirectDataInput;
import org.continuity.annotation.dsl.ann.ExtractedInput;
import org.continuity.annotation.dsl.ann.InterfaceAnnotation;
import org.continuity.annotation.dsl.ann.ParameterAnnotation;
import org.continuity.annotation.dsl.ann.PropertyOverride;
import org.continuity.annotation.dsl.ann.PropertyOverrideKey;
import org.continuity.annotation.dsl.ann.RegExExtraction;
import org.continuity.annotation.dsl.ann.SystemAnnotation;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.HttpParameterType;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.continuity.annotation.dsl.test.ContinuityModelTestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Henning Schulz
 *
 */
public enum AnnotationValidityTestInstance {

	FIRST("http://first/") {
		@Override
		public SystemModel getSystemModel() {
			return ContinuityModelTestInstance.SIMPLE.getSystemModel();
		}

		@Override
		public SystemAnnotation getAnnotation() {
			return ContinuityModelTestInstance.SIMPLE.getAnnotation();
		}
	},
	SECOND_SYSTEM("http://second/") {
		@Override
		public SystemModel getSystemModel() {
			return ULTIMATE_ANNOTATION.getSystemModel();
		}

		@Override
		public SystemAnnotation getAnnotation() {
			return ContinuityModelTestInstance.SIMPLE.getAnnotation();
		}
	},
	THIRD_SYSTEM("http://third/") {
		@Override
		public SystemModel getSystemModel() {
			SystemModel system = new SystemModel();
			HttpInterface interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("logout");
			system.addInterface(interf);

			return system;
		}

		@Override
		public SystemAnnotation getAnnotation() {
			return ContinuityModelTestInstance.SIMPLE.getAnnotation();
		}
	},
	SECOND_ANNOTATION("http://second-ann") {

		@Override
		protected SystemModel getSystemModel() {
			return ContinuityModelTestInstance.SIMPLE.getSystemModel();
		}

		@Override
		protected SystemAnnotation getAnnotation() {
			SystemAnnotation annotation = new SystemAnnotation();

			WeakReference<ServiceInterface<?>> interfRef = WeakReference.create(ServiceInterface.GENERIC_TYPE, "login");

			annotation = new SystemAnnotation();
			annotation.setId("ANN");

			InterfaceAnnotation interfaceAnn = new InterfaceAnnotation();
			interfaceAnn.setAnnotatedInterface(interfRef);
			PropertyOverride<PropertyOverrideKey.InterfaceLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpInterface.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);
			annotation.getInterfaceAnnotations().add(interfaceAnn);

			return annotation;
		}

	},
	THIRD_ANNOTATION("http://third-ann") {

		@Override
		protected SystemModel getSystemModel() {
			return ContinuityModelTestInstance.SIMPLE.getSystemModel();
		}

		@Override
		protected SystemAnnotation getAnnotation() {
			SystemAnnotation annotation = new SystemAnnotation();

			WeakReference<ServiceInterface<?>> interfRef = WeakReference.create(ServiceInterface.GENERIC_TYPE, "logout");

			annotation = new SystemAnnotation();
			annotation.setId("ANN");

			InterfaceAnnotation interfaceAnn = new InterfaceAnnotation();
			interfaceAnn.setAnnotatedInterface(interfRef);
			PropertyOverride<PropertyOverrideKey.InterfaceLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpInterface.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);
			annotation.getInterfaceAnnotations().add(interfaceAnn);

			return annotation;
		}

	},
	ULTIMATE_ANNOTATION("http://ultimate") {
		@Override
		protected SystemModel getSystemModel() {
			SystemModel system = new SystemModel();

			HttpInterface interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("logoutuser");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			system.addInterface(interf);

			HttpInterface interf2 = new HttpInterface();
			interf2.setDomain("mydomain");
			interf2.setId("logout");

			HttpParameter param2 = new HttpParameter();
			param2.setId("user");
			param2.setParameterType(HttpParameterType.REQ_PARAM);
			interf2.getParameters().add(param2);

			system.addInterface(interf2);

			return system;
		}

		@Override
		protected SystemAnnotation getAnnotation() {
			SystemAnnotation annotation = new SystemAnnotation();

			WeakReference<ServiceInterface<?>> interfRef = WeakReference.create(ServiceInterface.GENERIC_TYPE, "login");
			WeakReference<ServiceInterface<?>> interf2Ref = WeakReference.create(ServiceInterface.GENERIC_TYPE, "logout");
			WeakReference<Parameter> paramRef = WeakReference.create(Parameter.class, "user");
			WeakReference<Parameter> param2Ref = WeakReference.create(Parameter.class, "logoutuser");

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
			extrInput.setId("EXTRACTED");
			RegExExtraction extr = new RegExExtraction();
			extr.setFrom(interfRef);
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
			interfaceAnn.setAnnotatedInterface(interfRef);
			PropertyOverride<PropertyOverrideKey.InterfaceLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpInterface.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);

			ParameterAnnotation paramAnn = new ParameterAnnotation();
			paramAnn.setAnnotatedParameter(paramRef);
			paramAnn.setInput(input);

			interfaceAnn.getParameterAnnotations().add(paramAnn);
			annotation.getInterfaceAnnotations().add(interfaceAnn);

			InterfaceAnnotation interface2Ann = new InterfaceAnnotation();
			interface2Ann.setAnnotatedInterface(interf2Ref);

			ParameterAnnotation param2Ann = new ParameterAnnotation();
			param2Ann.setAnnotatedParameter(param2Ref);
			param2Ann.setInput(extrInput);
			interface2Ann.addParameterAnnotation(param2Ann);

			annotation.getInterfaceAnnotations().add(interface2Ann);

			return annotation;
		}
	};

	private final String link;

	private AnnotationValidityTestInstance(String link) {
		this.link = link;
	}

	protected abstract SystemModel getSystemModel();

	protected abstract SystemAnnotation getAnnotation();

	public ResponseEntity<SystemModel> getSystemEntity() {
		return new ResponseEntity<>(getSystemModel(), HttpStatus.OK);
	}

	public ResponseEntity<SystemAnnotation> getAnnotationEntity() {
		return new ResponseEntity<>(getAnnotation(), HttpStatus.OK);
	}

	public String getSystemLink() {
		return link + "system";
	}

	public String getAnnotationLink() {
		return link + "annotation";
	}

}
