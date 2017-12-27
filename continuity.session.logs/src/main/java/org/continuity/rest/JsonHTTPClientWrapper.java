package org.continuity.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;


/**
 * Wrapper class around the apache http client, allows sending and recieving requests / responses in JSON format. 
 * 
 * @author Jonas Kunz
 *
 */
public class JsonHTTPClientWrapper {

	/**
	 * The url of the server to access.
	 */
	private String host;

	/**
	 * the underlying http client.
	 */
	private CloseableHttpClient client;

	
	/**
	 * Initializes a new connection with the given connection information.
	 * @param url the url of the server
	 * @param user the username
	 * @param password the password
	 * @param proxy the proxy to use
	 */
	public JsonHTTPClientWrapper(String host) {
		this.host = host;
		connect();
	}
	

	/**
	 * private method for creating the connection.
	 */
	private void connect() {
	
		HttpClientBuilder clientFactory = HttpClients.custom();
		
		client = clientFactory.build();

	}

	/**
	 * closes the connection.
	 */
	public void destroy() {
		try {
			client.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Executes the given request, throws an error if the status is any
	 * different from 200.
	 * 
	 * @param request the request to execute.
	 * @return the response parsed into a JSON element
	 * @throws IOException 
	 */
	private <T> T executeRequest(HttpUriRequest request, Class<T> resultType) throws IOException {
		try {
			
			HttpClientContext context = HttpClientContext.create();
			HttpResponse response = client.execute(request, context);
			String jsonResponse = new BasicResponseHandler()
					.handleResponse(response);
			if (jsonResponse != null) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure( DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				return mapper.readValue(jsonResponse.getBytes(), resultType);
				
			} else {
				return null;
			}

		} catch (Exception e) {
			throw new IOException(e);
		}
	}
	
	
	public <T> T performGet(String path, Class<T> resultType) throws IOException {
		return performGet(path, resultType, new HashMap<String, String>());
	}

	
	public <T> T performGet(String path, Class<T> resultType, Map<String, String> parameters) throws IOException {
		URI requestTarget;
		try {
			URIBuilder builder = new URIBuilder("http://" + host + path);
			for (Entry<String, String> e: parameters.entrySet()) {
				builder.addParameter(e.getKey(), e.getValue());
			}
			requestTarget = builder.build();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		HttpGet getReq = new HttpGet(requestTarget);
		return executeRequest(getReq,resultType);
	}

	public CloseableHttpClient getHttpClient() {
		return client;
	}

	
	
	
}
