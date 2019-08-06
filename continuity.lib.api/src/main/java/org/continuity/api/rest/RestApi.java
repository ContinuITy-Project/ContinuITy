package org.continuity.api.rest;

import java.util.HashMap;
import java.util.Map;

import org.continuity.api.entities.exchange.MeasurementDataType;
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
		 * Configuration API of the orchestration service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Configuration {

			public static final String ROOT = "/config";

			/** {@value #ROOT}/{service}/{app-id:.+} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{service} */
			public static final RestEndpoint GET_ALL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ALL, RequestMethod.GET);

			/** {@value #ROOT} */
			public static final RestEndpoint POST = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.POST, RequestMethod.POST);

			private Configuration() {
			}

			public static class Paths {

				public static final String GET = "/{service}/{app-id:.+}";

				public static final String GET_ALL = "/{service}";

				public static final String POST = "";

				private Paths() {
				}
			}

		}

	}

	/**
	 * REST API of the IDPA service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Idpa {

		public static final String SERVICE_NAME = "idpa";

		private Idpa() {
		}

		/**
		 * Application API of the IDPA service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Application {

			public static final String ROOT = "/application";

			/** {@value #ROOT}/{app-id:.+} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{app-id:.+}/regex */
			public static final RestEndpoint GET_AS_REGEX = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_AS_REGEX, RequestMethod.GET);

			/** {@value #ROOT}/{app-id:.+}/delta */
			public static final RestEndpoint GET_DELTA = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_DELTA, RequestMethod.GET);

			/** {@value #ROOT}/{app-id:.+} */
			public static final RestEndpoint UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE, RequestMethod.POST);

			/** {@value #ROOT}/{app-id:.+}/workload-model */
			public static final RestEndpoint UPDATE_FROM_WORKLOAD_MODEL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_FROM_WORKLOAD_MODEL, RequestMethod.POST);

			private Application() {
			}

			public static class Paths {

				public static final String GET = "/{app-id:.+}";
				public static final String GET_AS_REGEX = "/{app-id:.+}/regex";
				public static final String GET_DELTA = "/{app-id:.+}/delta";
				public static final String UPDATE = "/{app-id:.+}";
				public static final String UPDATE_FROM_WORKLOAD_MODEL = "/{app-id:.+}/workload-model";

				private Paths() {
				}
			}
		}

		/**
		 * OpenAPI API of the IDPA service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class OpenApi {

			public static final String ROOT = "/openapi";

			/** {@value #ROOT}/{app-id:.+}/{version}/json */
			public static final RestEndpoint UPDATE_FROM_JSON = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_FROM_JSON, RequestMethod.POST);

			/** {@value #ROOT}/{app-id:.+}/{version}/url */
			public static final RestEndpoint UPDATE_FROM_URL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE_FROM_URL, RequestMethod.POST);

			private OpenApi() {
			}

			public static class Paths {

				public static final String UPDATE_FROM_JSON = "/{app-id:.+}/{version}/json";
				public static final String UPDATE_FROM_URL = "/{app-id:.+}/{version}/url";

				private Paths() {
				}
			}
		}

		/**
		 * Annotation API of the IDPA service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Annotation {

			public static final String ROOT = "/annotation";

			/** {@value #ROOT}/{app-id:.+} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{app-id:.+} */
			public static final RestEndpoint UPDATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPDATE, RequestMethod.POST);

			/** {@value #ROOT}/{app-id:.+} */
			public static final RestEndpoint UPLOAD = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPLOAD, RequestMethod.PUT);

			/** {@value #ROOT}/{app-id:.+}/broken */
			public static final RestEndpoint GET_BROKEN = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_BROKEN, RequestMethod.GET);

			private Annotation() {
			}

			public static class Paths {

				public static final String GET = "/{app-id:.+}";
				public static final String UPDATE = "/{app-id:.+}";
				public static final String UPLOAD = "/{app-id:.+}";
				public static final String GET_BROKEN = "/{app-id:.+}/broken";

				private Paths() {
				}
			}
		}

		public static class Version {

			public static final String ROOT = "/version";

			/** {@value #ROOT}/{app-id:.+}/latest */
			public static final RestEndpoint GET_LATEST = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_LATEST, RequestMethod.GET);

			private Version() {
			}

			public static class Paths {

				public static final String GET_LATEST = "/{app-id:.+}/latest";

				private Paths() {
				}

			}

		}

		/**
		 * Dummy API of the IDPA service.
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
		 * API for checking the availability.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Availability {

			public static final String ROOT = "/available";

			/** {@value #ROOT}/ */
			public static final RestEndpoint CHECK = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CHECK, RequestMethod.GET);

			public static class Paths {

				public static final String CHECK = "/";

				private Paths() {
				}
			}
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

			/** {@value #ROOT}/upload/{app-id:.+} */
			public static final RestEndpoint POST = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.POST, RequestMethod.POST);

			private TestPlan() {
			}

			public static class Paths {

				public static final String GET = "/{id}";

				public static final String POST = "/upload/{app-id:.+}";

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
	 * REST API of the BenchFlow service.
	 *
	 * @author Manuel Palenga
	 *
	 */
	public static class BenchFlow {

		public static final String SERVICE_NAME = "benchflow";

		private BenchFlow() {
		}

		/**
		 * DSL API of the BenchFlow service.
		 *
		 * @author Manuel Palenga
		 *
		 */
		public static class DSL {

			public static final String ROOT = "/loadtest";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			private DSL() {
			}

			public static class Paths {

				public static final String GET = "/{id}";

				private Paths() {
				}
			}
		}

		/**
		 * Report API of the BenchFlow service.
		 *
		 * @author Manuel Palenga
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
	 * REST API of the request rates service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class RequestRates {

		public static final String SERVICE_NAME = "request-rates";

		private RequestRates() {
		}

		/**
		 * JMeter API of the request rates service.
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
		 * Workload model API of the request rates service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class Model {

			public static final String ROOT = "/model";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint OVERVIEW = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.OVERVIEW, RequestMethod.GET);

			/** {@value #ROOT}/{id}/model */
			public static final RestEndpoint GET_MODEL = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_MODEL, RequestMethod.GET);

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint REMOVE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.REMOVE, RequestMethod.DELETE);

			/** {@value #ROOT}/{id}/application */
			public static final RestEndpoint GET_APPLICATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_APPLICATION, RequestMethod.GET);

			/** {@value #ROOT}/{id}/annotation */
			public static final RestEndpoint GET_ANNOTATION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_ANNOTATION, RequestMethod.GET);

			private Model() {
			}

			public static class Paths {

				public static final String OVERVIEW = "/{id}";
				public static final String GET_MODEL = "/{id}/model";
				public static final String REMOVE = "/{id}";
				public static final String GET_APPLICATION = "/{id}/application";
				public static final String GET_ANNOTATION = "/{id}/annotation";

				private Paths() {
				}
			}

		}

		/**
		 * Request logs API of the request rates service.
		 *
		 * @author Henning Schulz
		 *
		 */
		public static class RequestLogs {

			public static final String ROOT = "/requestlogs";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/ */
			public static final RestEndpoint UPLOAD = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.UPLOAD, RequestMethod.POST);

			private RequestLogs() {
			}

			public static class Paths {

				public static final String GET = "/{id}";
				public static final String UPLOAD = "/";

				private Paths() {
				}
			}

		}

	}

	/**
	 * REST API of the Cobra service.
	 *
	 * @author Henning Schulz
	 *
	 */
	public static class Cobra {

		public static final String SERVICE_NAME = "cobra";

		private Cobra() {
		}

		public static class Sessions {

			public static final String ROOT = "/sessions";

			/** {@value #ROOT}/{app-id:.+}/{tailoring}/simple */
			public static final RestEndpoint GET_SIMPLE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_SIMPLE, RequestMethod.GET);

			/** {@value #ROOT}/{app-id:.+}/{tailoring}/extended */
			public static final RestEndpoint GET_EXTENDED = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_SIMPLE, RequestMethod.GET);

			private Sessions() {
			}

			public static class Paths {

				public static final String GET_SIMPLE = "/{app-id:.+}/{tailoring}/simple";

				public static final String GET_EXTENDED = "/{app-id:.+}/{tailoring}/extended";

				private Paths() {
				}
			}
		}

		public static class MeasurementData {

			public static final String ROOT = "/measurement-data";

			/** {@value #ROOT}/{app-id:.+} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/{app-id:.+}/{version:.+} */
			public static final RestEndpoint GET_VERSION = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_VERSION, RequestMethod.GET);

			/** {@value #ROOT}/{app-id:.+}/{version:.+}/link */
			public static final RestEndpoint PUSH_LINK = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.PUSH_LINK, RequestMethod.POST);

			/** {@value #ROOT}/{app-id:.+}/{version:.+}/open-xtrace */
			public static final RestEndpoint PUSH_OPEN_XTRACE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.PUSH_OPEN_XTRACE, RequestMethod.POST);

			/** {@value #ROOT}/{app-id:.+}/{version:.+}/access-logs */
			public static final RestEndpoint PUSH_ACCESS_LOGS = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.PUSH_ACCESS_LOGS, RequestMethod.POST);

			/** {@value #ROOT}/{app-id:.+}/{version:.+}/csv */
			public static final RestEndpoint PUSH_CSV = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.PUSH_CSV, RequestMethod.POST);

			/** {@value #ROOT}/{app-id:.+}/{version:.+}/{type} */
			public static final Map<MeasurementDataType, RestEndpoint> PUSH_FOR_TYPE = new HashMap<>();

			static {
				PUSH_FOR_TYPE.put(MeasurementDataType.OPEN_XTRACE, PUSH_OPEN_XTRACE);
				PUSH_FOR_TYPE.put(MeasurementDataType.ACCESS_LOGS, PUSH_ACCESS_LOGS);
				PUSH_FOR_TYPE.put(MeasurementDataType.CSV, PUSH_CSV);
			}

			public static class Paths {

				public static final String GET = "/{app-id:.+}";

				public static final String GET_VERSION = "/{app-id:.+}/{version:.+}";

				public static final String PUSH_LINK = "/{app-id:.+}/{version:.+}/link";

				public static final String PUSH_OPEN_XTRACE = "/{app-id:.+}/{version:.+}/open-xtrace";

				public static final String PUSH_ACCESS_LOGS = "/{app-id:.+}/{version:.+}/access-logs";

				public static final String PUSH_CSV = "/{app-id:.+}/{version:.+}/csv";

				private Paths() {
				}

			}

		}

		public static class Context {

			public static final String ROOT = "/context";

			/** {@value #ROOT}/{app-id:.+} */
			public static final RestEndpoint PUSH = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.PUSH, RequestMethod.POST);

			private Context() {
			}

			public static class Paths {

				public static final String PUSH = "/{app-id:.+}";

				private Paths() {
				}

			}

		}

		public static class BehaviorModel {

			public static final String ROOT = "/behavior-model";

			/** {@value #ROOT}/create */
			public static final RestEndpoint CREATE = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.CREATE, RequestMethod.POST);

			private BehaviorModel() {
			}

			public static class Paths {

				public static final String CREATE = "/create";

				private Paths() {
				}

			}

		}

	}

	/**
	 * REST API of the Forecast service.
	 *
	 * @author Alper Hidiroglu
	 *
	 */
	public static class Forecast {

		public static final String SERVICE_NAME = "forecast";

		private Forecast() {
		}

		public static class ForecastResult {
			public static final String ROOT = "/forecastbundle";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			public static class Paths {

				public static final String GET = "/{id}";

				private Paths() {
				}
			}
		}
		public static class Context {
			public static final String ROOT = "/context";

			/** {@value #ROOT}/submit */
			public static final RestEndpoint SUBMIT = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.SUBMIT, RequestMethod.GET);

			public static class Paths {

				public static final String SUBMIT = "/submit";

				private Paths() {
				}
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
		 * BehaviorModel API of the WESSBAS service.
		 *
		 * @author Manuel Palenga, Henning Schulz
		 *
		 */
		public static class BehaviorModel {

			public static final String ROOT = "/behavior";

			/** {@value #ROOT}/{id} */
			public static final RestEndpoint GET = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET, RequestMethod.GET);

			/** {@value #ROOT}/legacy/{id} */
			public static final RestEndpoint GET_LEGACY = RestEndpoint.of(SERVICE_NAME, ROOT, Paths.GET_LEGACY, RequestMethod.GET);

			private BehaviorModel() {
			}

			public static class Paths {

				public static final String GET = "/{id}";

				public static final String GET_LEGACY = "/legacy/{id}";

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
				public static final String UPLOAD = "/{app-id:.+}";
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
		 * [load-test-type]/upload/{app-id:.+}
		 *
		 * @see RestApi.JMeter.TestPlan#POST
		 */
		public static final Map<String, RestEndpoint> UPLOAD_LOAD_TEST = new HashMap<>();

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
			WORKLOAD_MODEL_LINK.put(Wessbas.SERVICE_NAME, Wessbas.Model.OVERVIEW);
			WORKLOAD_MODEL_LINK.put(RequestRates.SERVICE_NAME, RequestRates.Model.OVERVIEW);

			PERSIST_WORKLOAD_MODEL.put(Wessbas.SERVICE_NAME, Wessbas.Model.PERSIST);


			GET_LOAD_TEST.put(JMeter.SERVICE_NAME, JMeter.TestPlan.GET);
			GET_LOAD_TEST.put(BenchFlow.SERVICE_NAME, BenchFlow.DSL.GET);

			UPLOAD_LOAD_TEST.put(JMeter.SERVICE_NAME, JMeter.TestPlan.POST);

			GET_LOAD_TEST_REPORT.put(JMeter.SERVICE_NAME, JMeter.Report.GET);
			GET_LOAD_TEST_REPORT.put(BenchFlow.SERVICE_NAME, BenchFlow.Report.GET);

			DELETE_LOAD_TEST_REPORT.put(JMeter.SERVICE_NAME, JMeter.Report.DELETE);
		}

		private Generic() {
		}

	}

}
