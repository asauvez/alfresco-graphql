package fr.smile.alfresco.graphql.integrationtest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
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
		String expectedResponse = IOUtils.toString(getClass().getResourceAsStream("/expectedResponse.json"), StandardCharsets.UTF_8);
		
		assertCall(expectedResponse, query, false, true);
	}

	@Test
	public void testMutation() throws Exception {
		try {
			assertCall( // create
				"{\"data\":{\"node\":{\"sharedHome\":{\"foo\":{\"content\":{\"size\":11}}}}}}",
				"{ node { sharedHome { foo: addChildContent(name: \"Foo.txt\") { content(newValue: \"Hello world\") { size }} }}}");
			assertCall( // query
				"{\"data\":{\"node\":{\"sharedHome\":{\"foo\":{\"content\":{\"asString\":\"Hello world\"}}}}}}",
				"{ node { sharedHome { foo: childByName (name: \"Foo.txt\") { content { asString } } }}}");
		} finally {
			assertCall( // delete
				"{\"data\":{\"node\":{\"sharedHome\":{\"foo\":{\"delete\":true}}}}}",
				"{ node { sharedHome { foo: childByName (name: \"Foo.txt\") { delete } }}}");
		}
		assertCall( // test really deleted
				"{\"data\":{\"node\":{\"sharedHome\":{\"foo\":null}}}}",
				"{ node { sharedHome { foo: childByName (name: \"Foo.txt\") { title } }}}");
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
	
	private void assertCall(String expectedResponse, String query) throws IOException {
		assertCall(expectedResponse, query, true, false);
	}

	private void assertCall(String expectedResponse, String query, boolean mutation, boolean pretty) throws IOException {
		query = "{ \"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\", \"variables\": null }";
		
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
		String webscriptURL = getPlatformEndpoint() + "/graphql" + (mutation ? "_mutation" : "");
		HttpPost post = new HttpPost(webscriptURL);
		post.setEntity(new StringEntity(query, ContentType.create("application/json")));
		HttpResponse httpResponse = httpclient.execute(post);

		String body = EntityUtils.toString(httpResponse.getEntity());
		assertEquals("Incorrect HTTP Response Status " + httpResponse.getStatusLine() + "\n" + body, HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
		return body;
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