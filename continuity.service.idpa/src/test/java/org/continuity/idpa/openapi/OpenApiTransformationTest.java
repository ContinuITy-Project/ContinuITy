package org.continuity.idpa.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.continuity.commons.idpa.OpenApiToIdpaTransformer;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.Endpoint;
import org.continuity.idpa.application.HttpEndpoint;
import org.continuity.idpa.application.HttpParameter;
import org.continuity.idpa.application.HttpParameterType;
import org.continuity.idpa.application.Parameter;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

public class OpenApiTransformationTest {

	private Swagger swagger;

	private OpenApiToIdpaTransformer transformer;

	@Before
	public void setup() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/example-swagger.json");
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, Charset.defaultCharset());

		swagger = new SwaggerParser().parse(writer.toString());

		transformer = new OpenApiToIdpaTransformer();
	}

	@Test
	public void testForEqualSystemModels() throws JsonGenerationException, JsonMappingException, IOException {
		Application system = transformer.transform(swagger);

		assertThat(system.getEndpoints()).extracting(Endpoint::getId).containsExactlyInAnyOrder("viewUpdateAccountUsingGET", "base_account_POST", "viewCustomerAddressesUsingGET");

		String interfName = "viewUpdateAccountUsingGET";
		testInterfaceProperties(system, interfName, "localhost", "8080", "GET", "/base/account", "http");
		testHeaders(system, interfName, "Accept: application/json", "Content-Type: application/json");
		testParameterIds(system, interfName, "viewUpdateAccountUsingGET_emailAddress_REQ_PARAM", "viewUpdateAccountUsingGET_firstName_REQ_PARAM", "viewUpdateAccountUsingGET_lastName_REQ_PARAM");
		testParameterNamesAndType(system, interfName, HttpParameterType.REQ_PARAM, "emailAddress", "firstName", "lastName");

		interfName = "base_account_POST";
		testInterfaceProperties(system, interfName, "localhost", "8080", "POST", "/base/account", "http");
		testHeaders(system, interfName, "Accept: application/json", "Content-Type: application/json");
		testParameterIds(system, interfName, "base_account_POST_emailAddress_REQ_PARAM", "base_account_POST_firstName_REQ_PARAM", "base_account_POST_lastName_REQ_PARAM");
		testParameterNamesAndType(system, interfName, HttpParameterType.REQ_PARAM, "emailAddress", "firstName", "lastName");

		interfName = "viewCustomerAddressesUsingGET";
		testInterfaceProperties(system, interfName, "localhost", "8080", "GET", "/base/account/{id}/addresses", "https");
		testHeaders(system, interfName, "Accept: application/json", "Content-Type: application/json", "Accept: application/xml", "Content-Type: application/xml");
		testParameterIds(system, interfName, "viewCustomerAddressesUsingGET_id_URL_PART");
		testParameterNamesAndType(system, interfName, HttpParameterType.URL_PART, "id");

	}

	private void testInterfaceProperties(Application system, String interfaceName, String domain, String port, String method, String path, String protocol) {
		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpEndpoint) interf).extracting(HttpEndpoint::getDomain)
		.containsExactly(domain);

		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpEndpoint) interf).extracting(HttpEndpoint::getPort)
		.containsExactly(port);

		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpEndpoint) interf).extracting(HttpEndpoint::getMethod)
		.containsExactly(method);

		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpEndpoint) interf).extracting(HttpEndpoint::getPath)
		.containsExactly(path);

		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpEndpoint) interf).extracting(HttpEndpoint::getProtocol)
		.containsExactly(protocol);
	}

	private void testHeaders(Application system, String interfaceName, String... headers) {
		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpEndpoint) interf).flatExtracting(HttpEndpoint::getHeaders)
		.containsExactlyInAnyOrder(headers);
	}

	private void testParameterIds(Application system, String interfaceName, String... ids) {
		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).flatExtracting(Endpoint::getParameters).extracting(param -> (HttpParameter) param)
		.extracting(Parameter::getId).containsExactlyInAnyOrder(ids);
	}

	private void testParameterNamesAndType(Application system, String interfaceName, HttpParameterType type, String... names) {
		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).flatExtracting(Endpoint::getParameters).extracting(param -> (HttpParameter) param)
		.extracting(HttpParameter::getName).containsExactlyInAnyOrder(names);

		assertThat(system.getEndpoints()).filteredOn(interf -> interf.getId().equals(interfaceName)).flatExtracting(Endpoint::getParameters).extracting(param -> (HttpParameter) param)
		.extracting(HttpParameter::getParameterType).containsOnly(type);
	}

}
