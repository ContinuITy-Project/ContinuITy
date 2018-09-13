package org.continuity.benchflow.transform;

import java.util.List;

import org.continuity.idpa.WeakReference;
import org.continuity.idpa.annotation.ApplicationAnnotation;
import org.continuity.idpa.annotation.CsvInput;
import org.continuity.idpa.annotation.DirectListInput;
import org.continuity.idpa.annotation.EndpointAnnotation;
import org.continuity.idpa.annotation.ExtractedInput;
import org.continuity.idpa.annotation.Input;
import org.continuity.idpa.annotation.ParameterAnnotation;
import org.continuity.idpa.annotation.PropertyOverride;
import org.continuity.idpa.annotation.PropertyOverrideKey;
import org.continuity.idpa.annotation.RegExExtraction;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;

public class ContinuITyIDPAGenerator {

	public Application setupApplication() {
		Application system = new Application();

		HttpEndpoint interf1 = initHttpEndpoint("localhost", "8080", "POST", "/login", "loginUsingPOST", "http");
		interf1.getHeaders().add("Content-Type: application/x-www-form-urlencoded");
		system.addEndpoint(interf1);	
		addHttpParamter(interf1, HttpParameterType.REQ_PARAM, "user_REQ_PARAM", "user");
		addHttpParamter(interf1, HttpParameterType.REQ_PARAM, "password_REQ_PARAM", "password");
		
		HttpEndpoint interf2 = initHttpEndpoint("localhost", "8080", "POST", "/search", "searchUsingPOST", "http");
		system.addEndpoint(interf2);		
		addHttpParamter(interf2, HttpParameterType.FORM, "item_FORM", "item");
		addHttpParamter(interf2, HttpParameterType.FORM, "color_FORM", "color");
		
		HttpEndpoint interf3 = initHttpEndpoint("localhost", "8080", "GET", "/buy", "buyUsingGET", "http");
		system.addEndpoint(interf3);			
		addHttpParamter(interf3, HttpParameterType.REQ_PARAM, "token_REQ_PARAM", "token");
		
		HttpEndpoint interf4 = initHttpEndpoint("localhost", "8080", "GET", "/shop/{ product }/select/{color}", "selectUsingGET", "http");
		system.addEndpoint(interf4);			
		addHttpParamter(interf4, HttpParameterType.URL_PART, "product_URL_PART", "product");
		addHttpParamter(interf4, HttpParameterType.URL_PART, "color_URL_PART", "color");
		
		HttpEndpoint interf5 = initHttpEndpoint("localhost", "8080", "POST", "/itemselection/{  category   }", "itemSelectionUsingPOST", "https");
		system.addEndpoint(interf5);			
		addHttpParamter(interf5, HttpParameterType.URL_PART, "category_URL_PARAM", "category");
		addHttpParamter(interf5, HttpParameterType.REQ_PARAM, "id_REQ_PARAM", "id");
		addHttpParamter(interf5, HttpParameterType.FORM, "price_FORM", "price");
		
		HttpEndpoint interf6 = initHttpEndpoint("localhost", "8080", "POST", "/account", "accountUsingPOST", "http");
		system.addEndpoint(interf6);		
		addHttpParamter(interf6, HttpParameterType.FORM, "token_FORM", "token");
		addHttpParamter(interf6, HttpParameterType.FORM, "item_FORM", "item");		
		
		HttpEndpoint interf7 = initHttpEndpoint("localhost", "8080", "POST", "/convert", "convertUsingPOST", "http");
		system.addEndpoint(interf7);		
		addHttpParamter(interf7, HttpParameterType.BODY, "content_xml_BODY", "content_xml");	
		
		HttpEndpoint interf8 = initHttpEndpoint("localhost", "8080", "POST", "/transform", "transformUsingPOST", "http");
		system.addEndpoint(interf8);		
		addHttpParamter(interf8, HttpParameterType.BODY, "content_json_BODY", "content_json");

		system.addEndpoint(initHttpEndpoint("localhost", "8080", "GET", "/logout", "logoutUsingGET", "http"));
		system.addEndpoint(initHttpEndpoint("localhost", "8080", "POST", "/product", "productUsingPOST", "http"));
		system.addEndpoint(initHttpEndpoint("localhost", "8080", "OPTIONS", "/index.html/start", "startUsingOPTIONS", "http"));
		
		return system;
	}
	
	private HttpEndpoint initHttpEndpoint(String domain, String port, String method, String path, String id, String protocol) {
		HttpEndpoint endpoint = new HttpEndpoint();
		endpoint.setDomain(domain);
		endpoint.setPort(port);
		endpoint.setMethod(method);
		endpoint.setPath(path);
		endpoint.setId(id);
		endpoint.setProtocol(protocol);
		return endpoint;
	}
	
	private void addHttpParamter(HttpEndpoint endpoint, HttpParameterType type, String id, String name) {
		HttpParameter param = new HttpParameter();
		param.setId(id);
		param.setName(name);
		param.setParameterType(type);
		endpoint.getParameters().add(param);
	}

	public ApplicationAnnotation setupAnnotation(Application system) {

		// Get all endpoints	
		List<Endpoint<?>> endpoints = system.getEndpoints();
		HttpEndpoint interfaceLogin = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("loginUsingPOST")).findAny().get();
		HttpEndpoint interfaceSearch = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("searchUsingPOST")).findAny().get();
		HttpEndpoint interfaceBuy = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("buyUsingGET")).findAny().get();
		HttpEndpoint interfaceSelect = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("selectUsingGET")).findAny().get();
		HttpEndpoint interfaceItem = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("itemSelectionUsingPOST")).findAny().get();
		HttpEndpoint interfaceAccount = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("accountUsingPOST")).findAny().get();
		
		// With body parameters
		HttpEndpoint interfaceConvert = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("convertUsingPOST")).findAny().get();
		HttpEndpoint interfaceTransform = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("transformUsingPOST")).findAny().get();
		
		// Without parameters
		HttpEndpoint interfaceLogout = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("logoutUsingGET")).findAny().get();
		HttpEndpoint interfaceProduct = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("productUsingPOST")).findAny().get();
		HttpEndpoint interfaceStart = (HttpEndpoint) endpoints.stream().filter(e -> e.getId().equals("startUsingOPTIONS")).findAny().get();
		
		// Input

		DirectListInput userElement = new DirectListInput();
		userElement.setId("Input_user_REQ_PARAM");
		userElement.getData().add("foo");
		userElement.getData().add("bar");

		CsvInput csvInput = new CsvInput();
		csvInput.setId("Input_csv");
		csvInput.setFilename("myfile.csv");
		csvInput.setColumn(3);
		csvInput.getAssociated().add(userElement);
		
		DirectListInput passwordElement = new DirectListInput();
		passwordElement.setId("Input_password_REQ_PARAM");
		passwordElement.getData().add("admin");

		ExtractedInput extrInputToken = new ExtractedInput();
		extrInputToken.setId("Input_extracted_token");
		RegExExtraction extr1 = new RegExExtraction();
		extr1.setFrom(WeakReference.create(interfaceLogin));
		extr1.setPattern("<input name=\"object\" type=\"hidden\" value=\"(.*)\"/>");
		extr1.setMatchNumber(4);
		extr1.setFallbackValue("OBJECT_NOT_FOUND");
		extrInputToken.getExtractions().add(extr1);
		RegExExtraction extr2 = new RegExExtraction();
		extr2.setFrom(WeakReference.create(interfaceAccount));
		extr2.setPattern("<input id=\"select\" name=\"object\" type=\"hidden\" value=\"(.*)\"/>");
		extrInputToken.getExtractions().add(extr2);
		
		ExtractedInput extrInputItem = new ExtractedInput();
		extrInputItem.setId("Input_extracted_item");
		RegExExtraction extr3 = new RegExExtraction();
		extr3.setFrom(WeakReference.create(interfaceLogin));
		extr3.setPattern("<input name=\"item\" type=\"hidden\" value=\"(.*)\"/>");
		extrInputItem.getExtractions().add(extr3);
		
		DirectListInput searchItemElement = new DirectListInput();
		searchItemElement.setId("Input_item_FORM");
		searchItemElement.getData().add("42");
		
		DirectListInput colorElement = new DirectListInput();
		colorElement.setId("Input_color");
		colorElement.getData().add("black");
		colorElement.getData().add("red");
		colorElement.getData().add("blue");
		
		DirectListInput selectProductElement = new DirectListInput();
		selectProductElement.setId("Input_product_URL_PART");
		selectProductElement.getData().add("car");
		selectProductElement.getData().add("bike");
			
		DirectListInput itemSelectionCategoryElement = new DirectListInput();
		itemSelectionCategoryElement.setId("Input_category_URL_PARAM");
		itemSelectionCategoryElement.getData().add("top");
		itemSelectionCategoryElement.getData().add("bottom");
		
		DirectListInput itemSelectionIdElement = new DirectListInput();
		itemSelectionIdElement.setId("Input_id_REQ_PARAM");
		itemSelectionIdElement.getData().add("123");
		
		DirectListInput itemSelectionPriceElement = new DirectListInput();
		itemSelectionPriceElement.setId("Input_price_FORM");
		itemSelectionPriceElement.getData().add("0.12");
		itemSelectionPriceElement.getData().add("12.34");
		itemSelectionPriceElement.getData().add("987.65");
		
		DirectListInput convertXmlElement = new DirectListInput();
		convertXmlElement.setId("Input_xml_BODY");
		convertXmlElement.getData().add("<xml><p>Hello</p></xml>");
		convertXmlElement.getData().add("<xml><div>World</div></xml>");
		
		ExtractedInput extrInputTransform = new ExtractedInput();
		extrInputTransform.setId("Input_extracted_content_json");
		RegExExtraction extr4 = new RegExExtraction();
		extr4.setFrom(WeakReference.create(interfaceConvert));
		extr4.setPattern("<div id=\"result\" value=\"(.*)\"/>");
		extrInputTransform.getExtractions().add(extr4);
		
		
		// Annotation

		ApplicationAnnotation annotation = new ApplicationAnnotation();
		annotation.getInputs().add(userElement);
		annotation.getInputs().add(csvInput);
		annotation.getInputs().add(extrInputToken);
		annotation.getInputs().add(extrInputItem);	
		annotation.getInputs().add(passwordElement);
		annotation.getInputs().add(searchItemElement);
		annotation.getInputs().add(colorElement);
		annotation.getInputs().add(selectProductElement);
		annotation.getInputs().add(itemSelectionIdElement);
		annotation.getInputs().add(itemSelectionCategoryElement);
		annotation.getInputs().add(itemSelectionPriceElement);
		annotation.getInputs().add(convertXmlElement);
		annotation.getInputs().add(extrInputTransform);
		annotation.setId("ANN");
	
		// Add parameters to endpoints
		EndpointAnnotation interfaceAnnLogin = createEndpointAnnotation(annotation, interfaceLogin);
		PropertyOverride<PropertyOverrideKey.EndpointLevel> ov = new PropertyOverride<>();
		ov.setKey(PropertyOverrideKey.HttpEndpoint.DOMAIN);
		ov.setValue("localhost");
		interfaceAnnLogin.addOverride(ov);

		addParameterAnnotation(interfaceAnnLogin, interfaceLogin, userElement, passwordElement);
		
		EndpointAnnotation interfaceAnnSearch = createEndpointAnnotation(annotation, interfaceSearch);		
		addParameterAnnotation(interfaceAnnSearch, interfaceSearch, searchItemElement, colorElement);
	
		EndpointAnnotation interfaceAnnBuy = createEndpointAnnotation(annotation, interfaceBuy);		
		addParameterAnnotation(interfaceAnnBuy, interfaceBuy, extrInputToken);
		
		EndpointAnnotation interfaceAnnSelect = createEndpointAnnotation(annotation, interfaceSelect);		
		addParameterAnnotation(interfaceAnnSelect, interfaceSelect, selectProductElement, colorElement);
		
		EndpointAnnotation interfaceAnnItem = createEndpointAnnotation(annotation, interfaceItem);		
		addParameterAnnotation(interfaceAnnItem, interfaceItem, itemSelectionCategoryElement, itemSelectionIdElement, itemSelectionPriceElement);
		
		EndpointAnnotation interfaceAnnAccount = createEndpointAnnotation(annotation, interfaceAccount);		
		addParameterAnnotation(interfaceAnnAccount, interfaceAccount, extrInputToken, extrInputItem);
		
		EndpointAnnotation interfaceAnnConvert = createEndpointAnnotation(annotation, interfaceConvert);		
		addParameterAnnotation(interfaceAnnConvert, interfaceConvert, convertXmlElement);	
		
		EndpointAnnotation interfaceAnnTransform = createEndpointAnnotation(annotation, interfaceTransform);		
		addParameterAnnotation(interfaceAnnTransform, interfaceTransform, extrInputTransform);	
		
		// Interfaces without parameters
		createEndpointAnnotation(annotation, interfaceLogout);
		createEndpointAnnotation(annotation, interfaceProduct);
		createEndpointAnnotation(annotation, interfaceStart);
		
		return annotation;
	}
	
	private EndpointAnnotation createEndpointAnnotation(ApplicationAnnotation annotation, HttpEndpoint endpoint) {
		EndpointAnnotation endpointAnnotation = new EndpointAnnotation();
		endpointAnnotation.setAnnotatedEndpoint(WeakReference.create(endpoint));
		annotation.getEndpointAnnotations().add(endpointAnnotation);
		return endpointAnnotation;
	}
	
	private void addParameterAnnotation(EndpointAnnotation endpointAnnotation, HttpEndpoint endpoint, Input... input) {
		for(int i = 0; i < input.length; i++) {
			ParameterAnnotation parameterAnnotation = new ParameterAnnotation();
			parameterAnnotation.setAnnotatedParameter(WeakReference.create(endpoint.getParameters().get(i)));
			parameterAnnotation.setInput(input[i]);
			endpointAnnotation.getParameterAnnotations().add(parameterAnnotation);
		}
	}
	
}
