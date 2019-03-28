package org.continuity.commons.idpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.commons.idpa.RequestUriMapper;
import org.continuity.idpa.application.Application;
import org.continuity.idpa.application.HttpEndpoint;
import org.junit.Before;
import org.junit.Test;

public class RequestUriMapperTest {

	private RequestUriMapper mapper;

	HttpEndpoint interf1;
	HttpEndpoint interf2;
	HttpEndpoint interf3;
	HttpEndpoint interf4;
	HttpEndpoint interf5;
	HttpEndpoint interf6;

	@Before
	public void setupSystemModel() {
		Application system = new Application();

		interf1 = new HttpEndpoint();
		interf1.setPath("/foo/bar/42");
		interf1.setMethod("GET");
		system.addEndpoint(interf1);

		interf2 = new HttpEndpoint();
		interf2.setPath("/a/bar");
		interf2.setMethod("GET");
		system.addEndpoint(interf2);

		interf3 = new HttpEndpoint();
		interf3.setPath("/foo/bar");
		interf3.setMethod("POST");
		system.addEndpoint(interf3);

		interf4 = new HttpEndpoint();
		interf4.setPath("/foo/{id}/bar");
		interf4.setMethod("POST");
		system.addEndpoint(interf4);

		interf5 = new HttpEndpoint();
		interf5.setPath("/foo/{id}/bar");
		interf5.setMethod("PUT");
		system.addEndpoint(interf5);

		interf6 = new HttpEndpoint();
		interf6.setPath("/foo/{id:*}");
		interf6.setMethod("DELETE");
		system.addEndpoint(interf6);

		mapper = new RequestUriMapper(system);
	}

	@Test
	public void testExactMapping() {
		assertThat(mapper.mapExactly("/foo/bar/42", "GET")).isEqualTo(interf1);
		assertThat(mapper.mapExactly("/a/bar/", "GET")).isEqualTo(interf2);
		assertThat(mapper.mapExactly("foo/bar/", "POST")).isEqualTo(interf3);
		assertThat(mapper.mapExactly("foo/{id}/bar", "POST")).isEqualTo(interf4);

		assertThat(mapper.mapExactly("/foo/bar/43", "GET")).isNull();
		assertThat(mapper.mapExactly("some/other/uri", "GET")).isNull();
		assertThat(mapper.mapExactly("/foo/bar/{}", "GET")).isNull();
		assertThat(mapper.mapExactly("/foo/id/bar", "GET")).isNull();

		assertThat(mapper.mapExactly("foo/{id}/bar", "PUT")).isEqualTo(interf5);
		assertThat(mapper.mapExactly("foo/{id}/bar", "GET")).isNull();
	}

	@Test
	public void testMappingRespectingWildcards() {
		assertThat(mapper.mapRespectingWildcards("/foo/bar/42", "GET")).isEqualTo(interf1);
		assertThat(mapper.mapRespectingWildcards("/foo/bar/{}", "GET")).isNull();
		assertThat(mapper.mapRespectingWildcards("/a/bar/", "GET")).isEqualTo(interf2);
		assertThat(mapper.mapRespectingWildcards("foo/bar/", "POST")).isEqualTo(interf3);
		assertThat(mapper.mapRespectingWildcards("foo/{id}/bar", "POST")).isEqualTo(interf4);

		assertThat(mapper.mapRespectingWildcards("foo/1/bar", "POST")).isEqualTo(interf4);
		assertThat(mapper.mapRespectingWildcards("foo/foo/bar", "POST")).isEqualTo(interf4);
		assertThat(mapper.mapRespectingWildcards("foo/{}/bar", "POST")).isEqualTo(interf4);
		assertThat(mapper.mapRespectingWildcards("foo/{ident}/bar", "POST")).isEqualTo(interf4);
		assertThat(mapper.mapRespectingWildcards("foo/{id}/{}/bar/", "POST")).isNull();

		assertThat(mapper.mapRespectingWildcards("foo/{id}/bar", "PUT")).isEqualTo(interf5);
		assertThat(mapper.mapRespectingWildcards("foo/{id}/bar", "GET")).isNull();

		assertThat(mapper.mapRespectingWildcards("/foo/bar", "DELETE")).isEqualTo(interf6);
		assertThat(mapper.mapRespectingWildcards("/foo/bar/baz", "DELETE")).isEqualTo(interf6);
		assertThat(mapper.mapRespectingWildcards("/foo", "DELETE")).isNull();
		assertThat(mapper.mapRespectingWildcards("/foo/", "DELETE")).isNull();
	}

}
