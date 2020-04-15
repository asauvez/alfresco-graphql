package fr.smile.alfresco.graphql.servlet;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

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
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class GraphQlServletIT {

	private static final String ACS_ENDPOINT_PROP = "acs.endpoint.path";
	private static final String ACS_DEFAULT_ENDPOINT = "http://localhost:8080/alfresco";

	@Test
	public void testWebScriptCall() throws Exception {
		String webscriptURL = getPlatformEndpoint() + "/graphql";

		// Login credentials for Alfresco Repo
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "admin");
		provider.setCredentials(AuthScope.ANY, credentials);

		// Create HTTP Client with credentials
		CloseableHttpClient httpclient = HttpClientBuilder.create()
				.setDefaultCredentialsProvider(provider)
				.build();

		String query = IOUtils.toString(getClass().getResourceAsStream("/query.json"), Charset.defaultCharset());
		String expectedResponse = IOUtils.toString(getClass().getResourceAsStream("/expectedResponse.json"), Charset.defaultCharset());
		
		query = "{ \"query\": \"" + query.replace("\"", "\\\"").replace("\n", " ") + "\", \"variables\": null }";
		
		// Execute Web Script call
		try {
			HttpPost post = new HttpPost(webscriptURL);
			post.setEntity(new StringEntity(query, ContentType.create("application/json")));
			HttpResponse httpResponse = httpclient.execute(post);

			String body = EntityUtils.toString(httpResponse.getEntity());
			assertEquals("Incorrect HTTP Response Status " + httpResponse.getStatusLine() + "\n" + body, HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonParser jp = new JsonParser();
			JsonElement je = jp.parse(body);
			String prettyJsonString = gson.toJson(je);
			assertEquals("Incorrect Web Script Response", expectedResponse, prettyJsonString);
		} finally {
			httpclient.close();
		}
	}

	private String getPlatformEndpoint() {
		final String platformEndpoint = System.getProperty(ACS_ENDPOINT_PROP);
		return StringUtils.isNotBlank(platformEndpoint) ? platformEndpoint : ACS_DEFAULT_ENDPOINT;
	}
}