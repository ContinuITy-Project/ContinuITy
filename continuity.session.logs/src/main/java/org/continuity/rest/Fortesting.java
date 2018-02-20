package org.continuity.rest;

import org.continuity.annotation.dsl.system.SystemModel;
import org.springframework.web.client.RestTemplate;

public class Fortesting {

	public static void main(String[] args) {
		RestTemplate restTemplate = new RestTemplate();
		SystemModel model = restTemplate.getForObject("http://localhost:61254/system/heat-clinic", SystemModel.class);

		System.out.println(model.getInterfaces());
	}

}
