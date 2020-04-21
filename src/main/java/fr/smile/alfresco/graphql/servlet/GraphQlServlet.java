package fr.smile.alfresco.graphql.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
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

import fr.smile.alfresco.graphql.helper.GraphQlConfigurationHelper;
import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;


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
			GraphQlServlet.GRAPHIQL_MUTATION_PATH
		})
public class GraphQlServlet extends GraphQLHttpServlet {
	
	static final String GRAPHQL_PATH			= "/graphql";
	static final String GRAPHQL_MUTATION_PATH	= "/graphql_mutation";
	static final String GRAPHIQL_PATH			= "/graphiql";
	static final String GRAPHIQL_MUTATION_PATH	= "/graphiql_mutation";

	private static Log log = LogFactory.getLog(GraphQlServlet.class);
	
	private QueryContext queryContext;
	private ServletAuthenticatorFactory servletAuthenticatorFactory;
	
	@Override
	public void init() {
		try {
			WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
			ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);

			queryContext = new QueryContext(serviceRegistry);
			servletAuthenticatorFactory = (ServletAuthenticatorFactory) applicationContext.getBean("webscripts.authenticator.remoteuser");
			
			super.init();
		} catch (RuntimeException ex) {
			log.error("GraphQL Init", ex);
		}
	}
	
	@Override
	protected GraphQLConfiguration getConfiguration() {
		return new GraphQlConfigurationHelper(queryContext).getConfiguration();
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
			
			boolean readOnly = ! request.getServletPath().startsWith(GRAPHQL_MUTATION_PATH);
			
			queryContext.getServiceRegistry().getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
				@Override
				public Void execute() throws Throwable {
					GraphQlServlet.super.service(request, response);
					
					return null;
				}
			}, readOnly, true);
		} finally {
			AuthenticationUtil.clearCurrentSecurityContext();
			queryContext.closeQuery();
		}
	}
}