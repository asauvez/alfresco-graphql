package fr.smile.alfresco.graphql.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
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

@WebServlet(name = "GraphQlServlet", urlPatterns = { "/graphql/*", "/graphiql" }, loadOnStartup = 1)
public class GraphQlServlet extends GraphQLHttpServlet {

	private static final String ALFRESCO_SCHEMA = "/alfresco/module/graphql/alfresco.graphqls";
	
	private QueryQl query;
	private ServletAuthenticatorFactory servletAuthenticatorFactory;

	@Override
	public void init() {
		WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
		query = new QueryQl(serviceRegistry);
		
		servletAuthenticatorFactory = (ServletAuthenticatorFactory) applicationContext.getBean("webscripts.authenticator.remoteuser");
		
		super.init();
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Authenticator authenticator = servletAuthenticatorFactory.create(
				new WebScriptServletRequest(null, request, null, null), 
				new WebScriptServletResponse(null, response));
		
		if (authenticator.authenticate(RequiredAuthentication.user, true)) {
			if ("/graphiql".equals(request.getServletPath())) {
				request.getRequestDispatcher("/graphiql.html").forward(request, response);
			} else {
				super.service(request, response);
			}
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
}