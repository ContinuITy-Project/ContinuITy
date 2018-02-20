package org.continuity.commons.workload.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import org.continuity.annotation.dsl.system.HttpInterface;
import org.continuity.annotation.dsl.system.SystemModel;
import org.junit.Before;
import org.junit.Test;

public class RequestUriMapperTest {

	private RequestUriMapper mapper;

	HttpInterface interf1;
	HttpInterface interf2;
	HttpInterface interf3;
	HttpInterface interf4;
	HttpInterface interf5;

	@Before
	public void setupSystemModel() {
		SystemModel system = new SystemModel();

		interf1 = new HttpInterface();
		interf1.setPath("/foo/bar/42");
		interf1.setMethod("GET");
		system.addInterface(interf1);

		interf2 = new HttpInterface();
		interf2.setPath("/a/bar");
		interf2.setMethod("GET");
		system.addInterface(interf2);

		interf3 = new HttpInterface();
		interf3.setPath("/foo/bar");
		interf3.setMethod("POST");
		system.addInterface(interf3);

		interf4 = new HttpInterface();
		interf4.setPath("/foo/{id}/bar");
		interf4.setMethod("POST");
		system.addInterface(interf4);

		interf5 = new HttpInterface();
		interf5.setPath("/foo/{id}/bar");
		interf5.setMethod("PUT");
		system.addInterface(interf5);

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
	}

}
