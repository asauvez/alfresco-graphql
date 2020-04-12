package fr.smile.alfresco.graphql.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import org.alfresco.service.ServiceRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class GraphQlService implements InitializingBean {

	@Autowired
	private ServiceRegistry serviceRegistry;

	private GraphQL build;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		SchemaParser schemaParser = new SchemaParser();
		try (Reader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/alfresco/module/graphql/alfresco.graphqls")))) {
			TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(reader);

			RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
					.type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world"))).build();

			SchemaGenerator schemaGenerator = new SchemaGenerator();
			GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

			build = GraphQL.newGraphQL(graphQLSchema).build();
		}
	}
	
	public String execute(String query) {
		ExecutionResult executionResult = build.execute(query);

		return executionResult.getData().toString();
	}
}