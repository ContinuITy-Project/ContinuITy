package org.continuity.idpa;

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Henning Schulz
 *
 */
public enum StaticIdpaTestInstance implements IdpaTestInstance {

	FIRST("http://first/") {
		@Override
		public Application getApplication() {
			Application model = new Application();
			model.setId(TAG);
			model.setTimestamp(new Date(86400000));

			HttpEndpoint interf = new HttpEndpoint();
			interf.setId("login");
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("user");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			model.addEndpoint(interf);
			return model;
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			ApplicationAnnotation ann = new ApplicationAnnotation();
			ann.setTimestamp(getApplication().getTimestamp());

			DirectListInput input = new DirectListInput();
			input.setId("input");
			input.setData(Arrays.asList("foo", "bar", "42"));
			ann.addInput(input);

			EndpointAnnotation endpAnn = new EndpointAnnotation();
			endpAnn.setAnnotatedEndpoint(WeakReference.create(Endpoint.GENERIC_TYPE, "login"));
			ann.getEndpointAnnotations().add(endpAnn);

			ParameterAnnotation paramAnn = new ParameterAnnotation();
			paramAnn.setAnnotatedParameter(WeakReference.create(Parameter.class, "user"));
			paramAnn.setInput(input);
			endpAnn.addParameterAnnotation(paramAnn);

			return ann;
		}
	},
	FIRST_REFINED("http://first_refined/") {

		@Override
		public Application getApplication() {
			Application app = FIRST.getApplication();
			app.setTimestamp(DateUtils.addHours(FIRST.getApplication().getTimestamp(), 1));
			return app;
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			ApplicationAnnotation ann = FIRST.getAnnotation();
			ann.setTimestamp(getApplication().getTimestamp());
			((DirectListInput) ann.getInputs().get(0)).setData(Arrays.asList("what", "ever"));
			return ann;
		}

	},
	SECOND("http://second/") {
		@Override
		public Application getApplication() {
			Application system = new Application();
			system.setId(TAG);
			system.setTimestamp(new Date(2 * 86400000));

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
			param2.setId("redirect");
			param2.setParameterType(HttpParameterType.REQ_PARAM);
			interf2.getParameters().add(param2);

			system.addEndpoint(interf2);

			return system;
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			ApplicationAnnotation ann = new ApplicationAnnotation();
			ann.setTimestamp(getApplication().getTimestamp());

			DirectListInput input = new DirectListInput();
			input.setId("input");
			input.setData(Arrays.asList("what", "ever"));
			ann.addInput(input);

			EndpointAnnotation endpAnn = new EndpointAnnotation();
			endpAnn.setAnnotatedEndpoint(WeakReference.create(Endpoint.GENERIC_TYPE, "login"));
			ann.getEndpointAnnotations().add(endpAnn);

			ParameterAnnotation paramAnn = new ParameterAnnotation();
			paramAnn.setAnnotatedParameter(WeakReference.create(Parameter.class, "logoutuser"));
			paramAnn.setInput(input);
			endpAnn.addParameterAnnotation(paramAnn);

			EndpointAnnotation endpAnn2 = new EndpointAnnotation();
			endpAnn2.setAnnotatedEndpoint(WeakReference.create(Endpoint.GENERIC_TYPE, "logout"));
			ann.getEndpointAnnotations().add(endpAnn2);

			ParameterAnnotation paramAnn2 = new ParameterAnnotation();
			paramAnn2.setAnnotatedParameter(WeakReference.create(Parameter.class, "redirect"));
			paramAnn2.setInput(input);
			endpAnn2.addParameterAnnotation(paramAnn2);

			return ann;
		}
	},
	THIRD("http://third/") {
		@Override
		public Application getApplication() {
			Application system = new Application();
			system.setId(TAG);
			system.setTimestamp(new Date(3 * 86400000));
			HttpEndpoint interf = new HttpEndpoint();
			interf.setDomain("mydomain");
			interf.setId("logout");
			system.addEndpoint(interf);

			return system;
		}

		@Override
		public ApplicationAnnotation getAnnotation() {
			ApplicationAnnotation ann = new ApplicationAnnotation();
			ann.setTimestamp(getApplication().getTimestamp());

			DirectListInput input = new DirectListInput();
			input.setId("input");
			input.setData(Arrays.asList("what", "ever"));
			ann.addInput(input);

			EndpointAnnotation endpAnn = new EndpointAnnotation();
			endpAnn.setAnnotatedEndpoint(WeakReference.create(Endpoint.GENERIC_TYPE, "logout"));
			ann.getEndpointAnnotations().add(endpAnn);

			return ann;
		}
	};

	public static final String TAG = "TEST";

	private final String link;

	private StaticIdpaTestInstance(String link) {
		this.link = link;
	}

	public ResponseEntity<Application> getEntity() {
		return new ResponseEntity<>(getApplication(), HttpStatus.OK);
	}

	public String getSystemLink() {
		return link + "system";
	}

}
