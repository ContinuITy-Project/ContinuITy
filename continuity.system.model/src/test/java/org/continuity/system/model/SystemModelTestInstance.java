package org.continuity.system.model;

import java.util.Date;

import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.HttpParameterType;
import org.continuity.annotation.dsl.system.SystemModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author Henning Schulz
 *
 */
public enum SystemModelTestInstance {

	FIRST("http://first/") {
		@Override
		public SystemModel get() {
			SystemModel model = new SystemModel();
			model.setId("FIRST");
			model.setTimestamp(new Date(86400000));

			HttpInterface interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("login");

			HttpParameter param = new HttpParameter();
			param.setId("user");
			param.setParameterType(HttpParameterType.REQ_PARAM);
			interf.getParameters().add(param);

			model.addInterface(interf);
			return model;
		}
	},
	SECOND("http://second/") {
		@Override
		public SystemModel get() {
			SystemModel system = new SystemModel();
			system.setId("SECOND");
			system.setTimestamp(new Date(2 * 86400000));

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
	},
	THIRD("http://third/") {
		@Override
		public SystemModel get() {
			SystemModel system = new SystemModel();
			system.setId("THIRD");
			system.setTimestamp(new Date(3 * 86400000));
			HttpInterface interf = new HttpInterface();
			interf.setDomain("mydomain");
			interf.setId("logout");
			system.addInterface(interf);

			return system;
		}
	};

	private final String link;

	private SystemModelTestInstance(String link) {
		this.link = link;
	}

	public abstract SystemModel get();

	public ResponseEntity<SystemModel> getEntity() {
		return new ResponseEntity<>(get(), HttpStatus.OK);
	}

	public String getSystemLink() {
		return link + "system";
	}

}
