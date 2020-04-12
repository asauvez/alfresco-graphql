package fr.smile.alfresco.graphql.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;

import javax.servlet.annotation.WebServlet;

import org.alfresco.service.ServiceRegistry;
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

@WebServlet(name = "GraphQlServlet", urlPatterns = { "/graphql/*" }, loadOnStartup = 1)
public class GraphQlServlet extends GraphQLHttpServlet {

	private QueryQl query;

	@Override
	public void init() {
		WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
		query = new QueryQl(serviceRegistry);
		
		super.init();
	}
	
	@Override
	protected GraphQLConfiguration getConfiguration() {
		try (BufferedInputStream schema = new BufferedInputStream(getClass().getResourceAsStream("/alfresco/module/graphql/alfresco.graphqls"))) {
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