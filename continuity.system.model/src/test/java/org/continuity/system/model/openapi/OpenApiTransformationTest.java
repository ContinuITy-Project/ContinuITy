package org.continuity.system.model.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.HttpParameter;
import org.continuity.annotation.dsl.system.HttpParameterType;
import org.continuity.annotation.dsl.system.Parameter;
import org.continuity.annotation.dsl.system.ServiceInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

public class OpenApiTransformationTest {

	private Swagger swagger;

	private OpenApiToContinuityTransformer transformer;

	@Before
	public void setup() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/example-swagger.json");
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, Charset.defaultCharset());

		swagger = new SwaggerParser().parse(writer.toString());

		transformer = new OpenApiToContinuityTransformer();
	}

	@Test
	public void testForEqualSystemModels() throws JsonGenerationException, JsonMappingException, IOException {
		SystemModel system = transformer.transform(swagger);

		assertThat(system.getInterfaces()).extracting(ServiceInterface::getId).containsExactlyInAnyOrder("viewUpdateAccountUsingGET", "base_account_POST", "viewCustomerAddressesUsingGET");

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

	private void testInterfaceProperties(SystemModel system, String interfaceName, String domain, String port, String method, String path, String protocol) {
		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpInterface) interf).extracting(HttpInterface::getDomain)
		.containsExactly(domain);

		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpInterface) interf).extracting(HttpInterface::getPort)
		.containsExactly(port);

		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpInterface) interf).extracting(HttpInterface::getMethod)
		.containsExactly(method);

		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpInterface) interf).extracting(HttpInterface::getPath)
		.containsExactly(path);

		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpInterface) interf).extracting(HttpInterface::getProtocol)
		.containsExactly(protocol);
	}

	private void testHeaders(SystemModel system, String interfaceName, String... headers) {
		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).extracting(interf -> (HttpInterface) interf).flatExtracting(HttpInterface::getHeaders)
		.containsExactlyInAnyOrder(headers);
	}

	private void testParameterIds(SystemModel system, String interfaceName, String... ids) {
		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).flatExtracting(ServiceInterface::getParameters).extracting(param -> (HttpParameter) param)
		.extracting(Parameter::getId).containsExactlyInAnyOrder(ids);
	}

	private void testParameterNamesAndType(SystemModel system, String interfaceName, HttpParameterType type, String... names) {
		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).flatExtracting(ServiceInterface::getParameters).extracting(param -> (HttpParameter) param)
		.extracting(HttpParameter::getName).containsExactlyInAnyOrder(names);

		assertThat(system.getInterfaces()).filteredOn(interf -> interf.getId().equals(interfaceName)).flatExtracting(ServiceInterface::getParameters).extracting(param -> (HttpParameter) param)
		.extracting(HttpParameter::getParameterType).containsOnly(type);
	}

}
