package org.continuity.session.logs.managers;

import org.springframework.web.client.RestTemplate;

/**
 * 
 * @author Alper Hi
 *
 */
public class SessionLogsPipelineManager {

	private String link;

	private RestTemplate restTemplate = new RestTemplate();

	public SessionLogsPipelineManager(String link) {
		this.link = link;
	}

	/**
	 * Runs the pipeline
	 * 
	 * @return
	 */
	public String runPipeline() {
		// getOPENXtrace(this.link);

		return generateIntoSessionLog();

	}

	// /**
	// * Gets OPEN.xtrace
	// */
	// public void getOPENXtrace(String link) {
	// String openxtrace = this.restTemplate.getForObject(link, String.class);
	// OPENxtraceDeserializer deserializer = new OPENxtraceDeserializer();
	// deserializer.deserialize(openxtrace);
	// }

	/**
	 * Input OPEN.xtrace, output Session Log String Stub
	 */
	public String generateIntoSessionLog() {

		String log = "DAC0E7CAC657D59A1328DEAC1F1F9472;\"ShopGET\":1511777946984000000:1511777947595000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:conversationId=1:<no-encoding>;\"HomeGET\":1511777963338000000:1511777963415000000:/dvdstore/home:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>;\"ShopGET\":1511779159657000000:1511779159856000000:/dvdstore/browse:8080:localhost:HTTP/1.1:GET:<no-query-string>:<no-encoding>";

		return log;
	}
}
