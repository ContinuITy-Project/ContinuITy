package org.continuity.idpa.application;

import java.util.Date;

import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Application;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Henning Schulz
 *
 */
public enum SystemModelTestInstance {

	FIRST("http://first/") {
		@Override
		public Application get() {
			Application model = new Application();
			model.setId("FIRST");
			model.setTimestamp(new Date(86400000));

			HttpEndpoint interf = new HttpEndpoint();
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("user");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			model.addEndpoint(interf);
			return model;
		}
	},
	SECOND("http://second/") {
		@Override
		public Application get() {
			Application system = new Application();
			system.setId("SECOND");
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
			param2.setId("user");
			param2.setParameterType(HttpParameterType.REQ_PARAM);
			interf2.getParameters().add(param2);

			system.addEndpoint(interf2);

			return system;
		}
	},
	THIRD("http://third/") {
		@Override
		public Application get() {
			Application system = new Application();
			system.setId("THIRD");
			system.setTimestamp(new Date(3 * 86400000));
			HttpEndpoint interf = new HttpEndpoint();
			interf.setDomain("mydomain");
			interf.setId("logout");
			system.addEndpoint(interf);

			return system;
		}
	};

	private final String link;

	private SystemModelTestInstance(String link) {
		this.link = link;
	}

	public abstract Application get();

	public ResponseEntity<Application> getEntity() {
		return new ResponseEntity<>(get(), HttpStatus.OK);
	}

	public String getSystemLink() {
		return link + "system";
	}

}
