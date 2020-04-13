package fr.smile.alfresco.graphql.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Authenticator;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.servlet.ServletAuthenticatorFactory;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import fr.smile.alfresco.graphql.model.QueryQl;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;


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
			GraphQlServlet.GRAPHIQL_PATH 
		})
public class GraphQlServlet extends GraphQLHttpServlet {
	
	static final String GRAPHQL_PATH = "/graphql";
	static final String GRAPHQL_MUTATION_PATH = "/graphql_mutation";
	static final String GRAPHIQL_PATH    = "/graphiql";

	private static Log log = LogFactory.getLog(GraphQlServlet.class);
	
	private static final String ALFRESCO_SCHEMA = "/alfresco/module/graphql/alfresco.graphqls";
	
	private QueryQl query;
	private ServletAuthenticatorFactory servletAuthenticatorFactory;
	private RetryingTransactionHelper retryingTransactionHelper;

	@Override
	public void init() {
		try {
			WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
			ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
			query = new QueryQl(serviceRegistry);
			
			servletAuthenticatorFactory = (ServletAuthenticatorFactory) applicationContext.getBean("webscripts.authenticator.remoteuser");
			retryingTransactionHelper = serviceRegistry.getRetryingTransactionHelper();

			super.init();
		} catch (RuntimeException ex) {
			log.error("GraphQL Init", ex);
		}
	}
	
	@Override
	protected GraphQLConfiguration getConfiguration() {
		try (BufferedInputStream schema = new BufferedInputStream(getClass().getResourceAsStream(ALFRESCO_SCHEMA))) {
			SchemaParser schemaParser = new SchemaParser();
			TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

			RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
					.type("Query", builder -> builder
						.dataFetcher("node", new StaticDataFetcher(query.getNode()))
					).build();

			SchemaGenerator schemaGenerator = new SchemaGenerator();
			GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

			return GraphQLConfiguration.with(graphQLSchema).build();
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
		
		if (GRAPHIQL_PATH.equals(request.getServletPath())) {
			request.getRequestDispatcher("/graphiql.html").forward(request, response);
			return;
		}
		
		boolean readOnly = ! request.getServletPath().startsWith(GRAPHQL_MUTATION_PATH);
		
		retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Void>() {
			@Override
			public Void execute() throws Throwable {
				GraphQlServlet.super.service(request, response);
				
				return null;
			}
		}, readOnly, true);
	}
}