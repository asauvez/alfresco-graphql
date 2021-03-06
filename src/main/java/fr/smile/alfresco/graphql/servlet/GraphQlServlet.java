package fr.smile.alfresco.graphql.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Authenticator;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.servlet.ServletAuthenticatorFactory;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.idl.RuntimeWiring;


/**
 * GraphQL endpoint
 * 
 * @author Adrien SAUVEZ
 *
 */
@WebServlet(
	name = "GraphQlServlet", 
	urlPatterns = { 
			GraphQlServlet.GRAPHQL_PATH, GraphQlServlet.GRAPHQL_PATH + "/*", 
			GraphQlServlet.GRAPHQL_MUTATION_PATH, GraphQlServlet.GRAPHQL_MUTATION_PATH + "/*", 
			GraphQlServlet.GRAPHIQL_PATH,
			GraphQlServlet.GRAPHIQL_MUTATION_PATH,
			GraphQlServlet.SCHEMA_PATH,
		})
public class GraphQlServlet extends GraphQLHttpServlet {
	
	private static final int REQUEST_SIZE_MAX = 32_000;
	
	static final String GRAPHQL_PATH			= "/graphql";
	static final String GRAPHQL_MUTATION_PATH	= "/graphql_mutation";
	static final String GRAPHIQL_PATH			= "/graphiql";
	static final String GRAPHIQL_MUTATION_PATH	= "/graphiql_mutation";
	static final String SCHEMA_PATH				= "/graphql.schema";

	private static Log log = LogFactory.getLog(GraphQlServlet.class);
	
	private QueryContext queryContext;
	private ServletAuthenticatorFactory servletAuthenticatorFactory;
	private GraphQlConfigurationBuilder configurationBuilder;
	
	@Override
	public void init() {
		if (configurationBuilder == null) {
			try {
				WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
				ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
	
				queryContext = new QueryContext(serviceRegistry);
				servletAuthenticatorFactory = (ServletAuthenticatorFactory) applicationContext.getBean("webscripts.authenticator.remoteuser");
				configurationBuilder = new GraphQlConfigurationBuilder(queryContext);
				
				super.init();
			} catch (RuntimeException ex) {
				log.error("GraphQL Init", ex);
			}
		}
	}
	
	@Override
	protected GraphQLConfiguration getConfiguration() {
		try {
			return configurationBuilder.getConfiguration();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Authenticator authenticator = servletAuthenticatorFactory.create(
				new WebScriptServletRequest(null, request, null, null), 
				new WebScriptServletResponse(null, response));
		if (! authenticator.authenticate(RequiredAuthentication.user, true)) {
			return;
		}
		try {
			if (GRAPHIQL_PATH.equals(request.getServletPath()) || GRAPHIQL_MUTATION_PATH.equals(request.getServletPath())) {
				String html = IOUtils.resourceToString("/META-INF/resources/graphiql.html", StandardCharsets.UTF_8);
				html = html.replace("#TARGET#", GRAPHIQL_MUTATION_PATH.equals(request.getServletPath()) 
						? "/alfresco/graphql_mutation" 
						: "/alfresco/graphql");
				response.setContentType("text/html");
				response.getWriter().write(html);
				return;
			}

			if (SCHEMA_PATH.equals(request.getServletPath())) {
				String schema = configurationBuilder.getSchema(RuntimeWiring.newRuntimeWiring());
				response.setContentType("text/plain");
				response.getWriter().write(schema);
				return;
			}

			boolean readOnly = ! request.getServletPath().startsWith(GRAPHQL_MUTATION_PATH);
						
			ContentCachingRequestWrapper contentCachingRequestWrapper = new ContentCachingRequestWrapper(request);
			ContentCachingResponseWrapper contentCachingResponseWrapper = new ContentCachingResponseWrapper(response);
			BufferedReader reader = contentCachingRequestWrapper.getReader();
			reader.mark(REQUEST_SIZE_MAX);

			queryContext.getServiceRegistry().getRetryingTransactionHelper()
				.doInTransaction(() -> {
					try {
						queryContext.executeQuery(() -> {
							GraphQlServlet.super.service(contentCachingRequestWrapper, contentCachingResponseWrapper);
							return null;
						});
						contentCachingResponseWrapper.copyBodyToResponse();
						return null;
					} catch (Throwable t) {
						reader.reset();
						contentCachingResponseWrapper.reset();
						throw t;
					}
				}, readOnly, true);
		} finally {
			AuthenticationUtil.clearCurrentSecurityContext();
		}
	}
}