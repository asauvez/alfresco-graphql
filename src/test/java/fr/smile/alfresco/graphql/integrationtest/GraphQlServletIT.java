package fr.smile.alfresco.graphql.integrationtest;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class GraphQlServletIT {

	private static final String ACS_ENDPOINT_PROP = "acs.endpoint.path";
	private static final String ACS_DEFAULT_ENDPOINT = "http://localhost:8080/alfresco";
	
	private CloseableHttpClient httpclient;
	
	@Test
	public void testWebScriptCall() throws Exception {
		String query = IOUtils.toString(getClass().getResourceAsStream("/query.graphql"), StandardCharsets.UTF_8);
		String expectedResponse = IOUtils.toString(getClass().getResourceAsStream("/queryResponse.json"), StandardCharsets.UTF_8);
		
		assertCall(expectedResponse, query, false, true);
	}

	@Test
	public void testMutation() throws Exception {
		StringBuilder expectedResponse = new StringBuilder();
		StringBuilder actualResponse = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/mutation.graphql"), StandardCharsets.UTF_8))) {
			String query;
			while ((query = reader.readLine()) != null) {
				if (! query.isBlank() && ! query.startsWith("//")) {
					expectedResponse.append(reader.readLine()).append("\n");
					actualResponse.append(callGraphQL(query, true)).append("\n");
				}
			}
		}
		assertEquals("Incorrect Web Script Response", expectedResponse.toString(), actualResponse.toString());
	}
	
	@Before
	public void init() {
		// Login credentials for Alfresco Repo
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin");
		provider.setCredentials(AuthScope.ANY, credentials);

		httpclient = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(provider)
				.build();
	}
	
	private void assertCall(String expectedResponse, String query, boolean mutation, boolean pretty) throws IOException {
		String body = callGraphQL(query, mutation);
		if (pretty) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonParser jp = new JsonParser();
			JsonElement je = jp.parse(body);
			body = gson.toJson(je);
		}
		assertEquals("Incorrect Web Script Response", expectedResponse, body);
	}
	
	private String callGraphQL(String query, boolean mutation) throws IOException {
		query = "{ \"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\", \"variables\": null }";

		String webscriptURL = getPlatformEndpoint() + "/graphql" + (mutation ? "_mutation" : "");
		HttpPost post = new HttpPost(webscriptURL);
		post.setEntity(new StringEntity(query, ContentType.create("application/json")));
		try {
			HttpResponse httpResponse = httpclient.execute(post);
	
			String body = EntityUtils.toString(httpResponse.getEntity());
			assertEquals("Incorrect HTTP Response Status " + httpResponse.getStatusLine() + "\n" + body, HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
			return body;
		} catch (HttpHostConnectException ex) {
			Assume.assumeTrue("Alfresco not started at " + webscriptURL, false);
			throw ex;
		}
	}
	
	@After
	public void close() throws IOException {
		httpclient.close();
	}

	private String getPlatformEndpoint() {
		final String platformEndpoint = System.getProperty(ACS_ENDPOINT_PROP);
		return StringUtils.isNotBlank(platformEndpoint) ? platformEndpoint : ACS_DEFAULT_ENDPOINT;
	}
}