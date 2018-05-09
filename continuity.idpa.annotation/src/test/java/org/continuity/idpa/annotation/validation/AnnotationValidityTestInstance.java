package org.continuity.idpa.annotation.validation;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Parameter;
import org.continuity.idpa.test.IdpaTestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Henning Schulz
 *
 */
public enum AnnotationValidityTestInstance {

	FIRST("http://first/") {
		@Override
		public Application getSystemModel() {
			return IdpaTestInstance.SIMPLE.getApplication();
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			return IdpaTestInstance.SIMPLE.getAnnotation();
		}
	},
	SECOND_SYSTEM("http://second/") {
		@Override
		public Application getSystemModel() {
			return ULTIMATE_ANNOTATION.getSystemModel();
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			return IdpaTestInstance.SIMPLE.getAnnotation();
		}
	},
	THIRD_SYSTEM("http://third/") {
		@Override
		public Application getSystemModel() {
			Application system = new Application();
			HttpEndpoint interf = new HttpEndpoint();
			interf.setDomain("mydomain");
			interf.setId("logout");
			system.addEndpoint(interf);

			return system;
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			return IdpaTestInstance.SIMPLE.getAnnotation();
		}
	},
	SECOND_ANNOTATION("http://second-ann") {

		@Override
		protected Application getSystemModel() {
			return IdpaTestInstance.SIMPLE.getApplication();
		}

		@Override
		protected ApplicationAnnotation getAnnotation() {
			ApplicationAnnotation annotation = new ApplicationAnnotation();

			WeakReference<Endpoint<?>> interfRef = WeakReference.create(Endpoint.GENERIC_TYPE, "login");

			annotation = new ApplicationAnnotation();
			annotation.setId("ANN");

			EndpointAnnotation interfaceAnn = new EndpointAnnotation();
			interfaceAnn.setAnnotatedEndpoint(interfRef);
			PropertyOverride<PropertyOverrideKey.EndpointLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpEndpoint.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);
			annotation.getEndpointAnnotations().add(interfaceAnn);

			return annotation;
		}

	},
	THIRD_ANNOTATION("http://third-ann") {

		@Override
		protected Application getSystemModel() {
			return IdpaTestInstance.SIMPLE.getApplication();
		}

		@Override
		protected ApplicationAnnotation getAnnotation() {
			ApplicationAnnotation annotation = new ApplicationAnnotation();

			WeakReference<Endpoint<?>> interfRef = WeakReference.create(Endpoint.GENERIC_TYPE, "logout");

			annotation = new ApplicationAnnotation();
			annotation.setId("ANN");

			EndpointAnnotation interfaceAnn = new EndpointAnnotation();
			interfaceAnn.setAnnotatedEndpoint(interfRef);
			PropertyOverride<PropertyOverrideKey.EndpointLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpEndpoint.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);
			annotation.getEndpointAnnotations().add(interfaceAnn);

			return annotation;
		}

	},
	ULTIMATE_ANNOTATION("http://ultimate") {
		@Override
		protected Application getSystemModel() {
			Application system = new Application();

			HttpEndpoint interf = new HttpEndpoint();
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("logoutuser");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			system.addEndpoint(interf);

			HttpEndpoint interf2 = new HttpEndpoint();
			interf2.setDomain("mydomain");
			interf2.setId("logout");

			HttpParameter param2 = new HttpParameter();
			param2.setId("user");
			param2.setParameterType(HttpParameterType.REQ_PARAM);
			interf2.getParameters().add(param2);

			system.addEndpoint(interf2);

			return system;
		}

		@Override
		protected ApplicationAnnotation getAnnotation() {
			ApplicationAnnotation annotation = new ApplicationAnnotation();

			WeakReference<Endpoint<?>> interfRef = WeakReference.create(Endpoint.GENERIC_TYPE, "login");
			WeakReference<Endpoint<?>> interf2Ref = WeakReference.create(Endpoint.GENERIC_TYPE, "logout");
			WeakReference<Parameter> paramRef = WeakReference.create(Parameter.class, "user");
			WeakReference<Parameter> param2Ref = WeakReference.create(Parameter.class, "logoutuser");

			// Input

			DirectListInput input = new DirectListInput();
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

			DirectListInput unknownInput = new DirectListInput();
			unknownInput.setId("UNK1");
			unknownInput.getData().add("something");

			// Annotation

			annotation = new ApplicationAnnotation();
			annotation.getInputs().add(input);
			annotation.getInputs().add(csvInput);
			annotation.getInputs().add(extrInput);
			annotation.getInputs().add(unknownInput);
			annotation.setId("ANN");

			EndpointAnnotation interfaceAnn = new EndpointAnnotation();
			interfaceAnn.setAnnotatedEndpoint(interfRef);
			PropertyOverride<PropertyOverrideKey.EndpointLevel> ov = new PropertyOverride<>();
			ov.setKey(PropertyOverrideKey.HttpEndpoint.DOMAIN);
			ov.setValue("localhost");
			interfaceAnn.addOverride(ov);

			ParameterAnnotation paramAnn = new ParameterAnnotation();
			paramAnn.setAnnotatedParameter(paramRef);
			paramAnn.setInput(input);

			interfaceAnn.getParameterAnnotations().add(paramAnn);
			annotation.getEndpointAnnotations().add(interfaceAnn);

			EndpointAnnotation interface2Ann = new EndpointAnnotation();
			interface2Ann.setAnnotatedEndpoint(interf2Ref);

			ParameterAnnotation param2Ann = new ParameterAnnotation();
			param2Ann.setAnnotatedParameter(param2Ref);
			param2Ann.setInput(extrInput);
			interface2Ann.addParameterAnnotation(param2Ann);

			annotation.getEndpointAnnotations().add(interface2Ann);

			return annotation;
		}
	};

	private final String link;

	private AnnotationValidityTestInstance(String link) {
		this.link = link;
	}

	protected abstract Application getSystemModel();

	protected abstract ApplicationAnnotation getAnnotation();

	public ResponseEntity<Application> getSystemEntity() {
		return new ResponseEntity<>(getSystemModel(), HttpStatus.OK);
	}

	public ResponseEntity<ApplicationAnnotation> getAnnotationEntity() {
		return new ResponseEntity<>(getAnnotation(), HttpStatus.OK);
	}

	public String getSystemLink() {
		return link + "system";
	}

	public String getAnnotationLink() {
		return link + "annotation";
	}

}
