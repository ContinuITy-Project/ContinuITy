package org.continuity.api.rest;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Holds the REST API of all ContinuITy services. This does not conform the principle of
 * microservices, but allows us as a small number of developers to handle multiple microservice-like
 * services.
 *
 * @author Henning Schulz
 *
 */
public class RestApi {

	// TODO: Change the JavaDoc comments of the RestEndpoints from hard-coded paths to {@value
	// Paths#PATH} references (does currently not work in Eclipse)

	private RestApi() {
	}

	/**
	 * REST API of the orchestrator service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Orchestrator {

		public static final String SERVICE_NAME = "orchestrator";

		private Orchestrator() {
		}

		/**
		 * Orchestration API of the orchestration service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Orchestration {

			public static final String ROOT = "/order";

			/** {@value #ROOT}/{id}/result */
			public static final RestEndpoint RESULT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.RESULT, RequestMethod.GET);

			/** {@value #ROOT}/{id}/wait */
			public static final RestEndpoint WAIT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.WAIT, RequestMethod.GET);

			/** {@value #ROOT}/submit */
			public static final RestEndpoint SUBMIT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.SUBMIT, RequestMethod.GET);

			private Orchestration() {
			}

			public static class Paths {

				public static final String RESULT = "/{id}/result";

				public static final String WAIT = "/{id}/wait";

				public static final String SUBMIT = "/submit";

				private Paths() {
				}
			}

		}

		/**
		 * IDPA API of the orchestration service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Idpa {

			public static final String ROOT = "/idpa";

			/** {@value #ROOT}/{tag}/application */
			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_APPLICATION, RequestMethod.GET);

			/** {@value #ROOT}/{tag}/annotation */
			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ANNOTATION, RequestMethod.GET);

			/** {@value #ROOT}/{tag}/application */
			public static final RestEndpoint UPDATE_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_APPLICATION, RequestMethod.POST);

			/** {@value #ROOT}/{tag}/openapi/{version}/json */
			public static final RestEndpoint UPDATE_APP_FROM_OPEN_API_JSON = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_APP_FROM_OPEN_API_JSON, RequestMethod.POST);

			/** {@value #ROOT}/{tag}/openapi/{version}/url */
			public static final RestEndpoint UPDATE_APP_FROM_OPEN_API_URL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_APP_FROM_OPEN_API_URL, RequestMethod.POST);

			/** {@value #ROOT}/{tag}/annotation */
			public static final RestEndpoint UPDATE_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_ANNOTATION, RequestMethod.POST);

			/** {@value #ROOT}/report */
			public static final RestEndpoint REPORT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.REPORT, RequestMethod.GET);

			private Idpa() {
			}

			public static class Paths {

				public static final String GET_APPLICATION = "/{tag}/application";
				public static final String GET_ANNOTATION = "/{tag}/annotation";
				public static final String UPDATE_APPLICATION = "/{tag}/application";
				public static final String UPDATE_APP_FROM_OPEN_API_JSON = "/{tag}/openapi/{version}/json";
				public static final String UPDATE_APP_FROM_OPEN_API_URL = "/{tag}/openapi/{version}/url";
				public static final String UPDATE_ANNOTATION = "/{tag}/annotation";
				public static final String REPORT = "/report";

				private Paths() {
				}
			}

		}

		/**
		 * Loadtest API of the frontend service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Loadtest {

			public static final String ROOT = "/loadtest";

			/** {@value #ROOT}/{type}/test/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{type}/report/{id} */
			public static final RestEndpoint REPORT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.REPORT, RequestMethod.GET);

			/** {@value #ROOT}/{type}/report/{id} */
			public static final RestEndpoint DELETE_REPORT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.DELETE_REPORT, RequestMethod.DELETE);

			private Loadtest() {
			}

			public static class Paths {

				public static final String GET = "/{type}/test/{id}";

				public static final String REPORT = "/{type}/report/{id}";

				public static final String DELETE_REPORT = "/{type}/test/{id}";

				private Paths() {
				}
			}

		}

		/**
		 * Session logs API of the orchestration service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class SessionLogs {

			public static final String ROOT = "/sessions";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			private SessionLogs() {
			}

			public static class Paths {

				public static final String GET = "/{id}";

				private Paths() {
				}
			}

		}

		/**
		 * Workload model API of the frontend service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class WorkloadModel {

			public static final String ROOT = "/workloadmodel";

			/** {@value #ROOT}/{type}/model/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{type}/model/{id}/persist */
			public static final RestEndpoint PERSIST = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.PERSIST, RequestMethod.POST);

			private WorkloadModel() {
			}

			public static class Paths {

				public static final String GET = "/{type}/model/{id}";
				public static final String PERSIST = "/{type}/model/{id}/persist";

				private Paths() {
				}
			}

		}

	}

	/**
	 * REST API of the IDPA annotation service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaAnnotation {

		public static final String SERVICE_NAME = "idpa-annotation";

		private IdpaAnnotation() {
		}

		/**
		 * Annotation API of the IDPA annotation service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Annotation {

			public static final String ROOT = "/annotation";

			/** {@value #ROOT}/{tag} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{tag}/base */
			public static final RestEndpoint GET_BASE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_BASE, RequestMethod.GET);

			/** {@value #ROOT}/{tag} */
			public static final RestEndpoint UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE, RequestMethod.POST);

			/** {@value #ROOT}/legacy/{tag}/update */
			public static final RestEndpoint LEGACY_UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.LEGACY_UPDATE, RequestMethod.GET);

			private Annotation() {
			}

			public static class Paths {

				public static final String GET = "/{tag}";
				public static final String GET_BASE = "/{tag}/base";
				public static final String UPDATE = "/{tag}";
				public static final String UPLOAD = "/{tag}";
				public static final String LEGACY_UPDATE = "/legacy/{tag}/update";

				private Paths() {
				}
			}
		}

		/**
		 * Dummy API of the IDPA annotation service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Dummy {

			public static final String ROOT = "/dummy/dvdstore";

			/** {@value #ROOT}/annotation */
			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_APPLICATION, RequestMethod.GET);

			/** {@value #ROOT}/application */
			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ANNOTATION, RequestMethod.GET);

			private Dummy() {
			}

			public static class Paths {

				public static final String GET_APPLICATION = "/annotation";
				public static final String GET_ANNOTATION = "/application";

				private Paths() {
				}
			}
		}

	}

	/**
	 * REST API of the IDPA application service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class IdpaApplication {

		public static final String SERVICE_NAME = "idpa-application";

		private IdpaApplication() {
		}

		/**
		 * Application API of the IDPA application service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Application {

			public static final String ROOT = "/application";

			/** {@value #ROOT}/{tag} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{tag}/delta */
			public static final RestEndpoint GET_DELTA = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_DELTA, RequestMethod.GET);

			/** {@value #ROOT}/{tag} */
			public static final RestEndpoint UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE, RequestMethod.POST);

			/** {@value #ROOT}/legacy/{tag}/update */
			public static final RestEndpoint LEGACY_UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.LEGACY_UPDATE, RequestMethod.GET);

			private Application() {
			}

			public static class Paths {

				public static final String GET = "/{tag}";
				public static final String GET_DELTA = "/{tag}/delta";
				public static final String UPDATE = "/{tag}";
				public static final String LEGACY_UPDATE = "/legacy/{tag}/update";

				private Paths() {
				}
			}
		}

		/**
		 * OpenAPI API of the IDPA application service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class OpenApi {

			public static final String ROOT = "/openapi";

			/** {@value #ROOT}/{tag}/{version}/json */
			public static final RestEndpoint UPDATE_FROM_JSON = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_FROM_JSON, RequestMethod.POST);

			/** {@value #ROOT}/{tag}/{version}/url */
			public static final RestEndpoint UPDATE_FROM_URL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_FROM_URL, RequestMethod.POST);

			private OpenApi() {
			}

			public static class Paths {

				public static final String UPDATE_FROM_JSON = "/{tag}/{version}/json";
				public static final String UPDATE_FROM_URL = "/{tag}/{version}/url";

				private Paths() {
				}
			}
		}

	}

	/**
	 * REST API of the JMeter service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class JMeter {

		public static final String SERVICE_NAME = "jmeter";

		private JMeter() {
		}

		/**
		 * Test plan API of the JMeter service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class TestPlan {

			public static final String ROOT = "/loadtest";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			private TestPlan() {
			}

			public static class Paths {

				public static final String GET = "/{id}";

				private Paths() {
				}
			}
		}

		/**
		 * Report API of the JMeter service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Report {

			public static final String ROOT = "/report";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint DELETE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.DELETE, RequestMethod.DELETE);

			private Report() {
			}

			public static class Paths {

				public static final String GET = "/{id}";

				public static final String DELETE = "/{id}";

				private Paths() {
				}
			}
		}

	}

	/**
	 * REST API of the Session Logs service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class SessionLogs {

		public static final String SERVICE_NAME = "session-logs";

		public static final String ROOT = "/sessions";

		/** {@value #ROOT}/{id} */
		public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

		private SessionLogs() {
		}

		public static class Paths {

			public static final String GET = "/{id}";

			private Paths() {
			}
		}

	}

	/**
	 * REST API of the WESSBAS service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Wessbas {

		public static final String SERVICE_NAME = "wessbas";

		private Wessbas() {
		}

		/**
		 * JMeter API of the WESSBAS service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class JMeter {

			public static final String ROOT = "/jmeter";

			/** {@value #ROOT}/{id}/create */
			public static final RestEndpoint CREATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CREATE, RequestMethod.GET);

			private JMeter() {
			}

			public static class Paths {

				public static final String CREATE = "/{id}";

				private Paths() {
				}
			}

		}

		/**
		 * Workload model API of the WESSBAS service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Model {

			public static final String ROOT = "/model";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint OVERVIEW = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.OVERVIEW, RequestMethod.GET);

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint REMOVE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.REMOVE, RequestMethod.DELETE);

			/** {@value #ROOT}/{id}/application */
			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_APPLICATION, RequestMethod.GET);

			/** {@value #ROOT}/{id}/annotation */
			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ANNOTATION, RequestMethod.GET);

			/** {@value #ROOT}/{id}/persist */
			public static final RestEndpoint PERSIST = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.PERSIST, RequestMethod.POST);

			private Model() {
			}

			public static class Paths {

				public static final String OVERVIEW = "/{id}";
				public static final String UPLOAD = "/{tag}";
				public static final String REMOVE = "/{id}";
				public static final String GET_APPLICATION = "/{id}/application";
				public static final String GET_ANNOTATION = "/{id}/annotation";
				public static final String PERSIST = "/{id}/persist";

				private Paths() {
				}
			}

		}

	}

	/**
	 * Generic, abstract REST endpoints. The implementing endpoints can be retrieved via the
	 * {@link Map#get(Object)} method, e.g., the JMeter {@link Model#OVERVIEW} via
	 * {@link Generic#WORKLOAD_MODEL_LINK}<code>.get("jmeter")</code>.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Generic {

		/**
		 * [workload-model-type]/model/{id}
		 *
		 * @see RestApi.Wessbas.Model#OVERVIEW
		 */
		public static final Map<String, RestEndpoint> WORKLOAD_MODEL_LINK = new HashMap<>();

		/**
		 * [workload-model-type]/model/{id}/persist
		 *
		 * @see RestApi.Wessbas.Model#PERSIST
		 */
		public static final Map<String, RestEndpoint> PERSIST_WORKLOAD_MODEL = new HashMap<>();

		/**
		 * [load-test-type]/loadtest/{id}
		 *
		 * @see RestApi.JMeter.TestPlan#GET
		 */
		public static final Map<String, RestEndpoint> GET_LOAD_TEST = new HashMap<>();

		/**
		 * [load-test-type]/report/{id}
		 *
		 * @see RestApi.JMeter.Report#GET
		 */
		public static final Map<String, RestEndpoint> GET_LOAD_TEST_REPORT = new HashMap<>();

		/**
		 * [load-test-type]/report/{id}
		 *
		 * @see RestApi.JMeter.Report#DELETE
		 */
		public static final Map<String, RestEndpoint> DELETE_LOAD_TEST_REPORT = new HashMap<>();

		static {
			WORKLOAD_MODEL_LINK.put("wessbas", Wessbas.Model.OVERVIEW);

			PERSIST_WORKLOAD_MODEL.put("wessbas", Wessbas.Model.PERSIST);

			GET_LOAD_TEST.put("jmeter", JMeter.TestPlan.GET);

			GET_LOAD_TEST_REPORT.put("jmeter", JMeter.Report.GET);

			DELETE_LOAD_TEST_REPORT.put("jmeter", JMeter.Report.DELETE);
		}

		private Generic() {
		}

	}

}
