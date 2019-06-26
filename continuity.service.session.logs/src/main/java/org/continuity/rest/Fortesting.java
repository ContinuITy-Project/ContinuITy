package org.continuity.rest;

import org.continuity.idpa.application.Application;
import org.springframework.web.client.RestTemplate;

public class Fortesting {

	public static void main(String[] args) {
		RestTemplate restTemplate = new RestTemplate();
		Application model = restTemplate.getForObject("http://localhost:61254/system/heat-clinic", Application.class);

		System.out.println(model.getEndpoints());
	}

}
