package fr.smile.alfresco.graphql.servlet;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
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

import fr.smile.alfresco.graphql.model.ContentReaderQL;
import fr.smile.alfresco.graphql.model.DateQL;
import fr.smile.alfresco.graphql.model.NodeQL;
import fr.smile.alfresco.graphql.model.QueryQL;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
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
	
	private QueryQL query;
	private ServletAuthenticatorFactory servletAuthenticatorFactory;
	private RetryingTransactionHelper retryingTransactionHelper;
	private DictionaryService dictionaryService;

	private Map<QName, String> typeByDataType = new HashMap<>();
	private Map<QName, Function<Serializable, Object>> transformByDataType = new HashMap<>();
	
	@Override
	public void init() {
		try {
			WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
			ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
			query = new QueryQL(serviceRegistry);
			
			servletAuthenticatorFactory = (ServletAuthenticatorFactory) applicationContext.getBean("webscripts.authenticator.remoteuser");
			retryingTransactionHelper = serviceRegistry.getRetryingTransactionHelper();
			dictionaryService = serviceRegistry.getDictionaryService();
			NamespaceService namespaceService = serviceRegistry.getNamespaceService();

			typeByDataType.put(DataTypeDefinition.TEXT, "String");
			typeByDataType.put(DataTypeDefinition.ANY, "String");
			typeByDataType.put(DataTypeDefinition.ENCRYPTED, "String");
			typeByDataType.put(DataTypeDefinition.MLTEXT, "String");
			typeByDataType.put(DataTypeDefinition.CONTENT, "ContentData");
			typeByDataType.put(DataTypeDefinition.INT, "Int");
			typeByDataType.put(DataTypeDefinition.LONG, "Int");
			typeByDataType.put(DataTypeDefinition.FLOAT, "Float");
			typeByDataType.put(DataTypeDefinition.DOUBLE, "Float");
			typeByDataType.put(DataTypeDefinition.DATE, "Date");
			typeByDataType.put(DataTypeDefinition.DATETIME, "Date");
			typeByDataType.put(DataTypeDefinition.BOOLEAN, "Boolean");
			typeByDataType.put(DataTypeDefinition.QNAME, "String");
			typeByDataType.put(DataTypeDefinition.CATEGORY, "String");
			typeByDataType.put(DataTypeDefinition.NODE_REF, "ID");
			typeByDataType.put(DataTypeDefinition.CHILD_ASSOC_REF, "String");
			typeByDataType.put(DataTypeDefinition.ASSOC_REF, "String");
			typeByDataType.put(DataTypeDefinition.PATH, "String");
			typeByDataType.put(DataTypeDefinition.LOCALE, "String");
			typeByDataType.put(DataTypeDefinition.PERIOD, "String");
			
			transformByDataType.put(DataTypeDefinition.TEXT, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.ANY, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.ENCRYPTED, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.MLTEXT, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.CONTENT, o -> o); // special case
			transformByDataType.put(DataTypeDefinition.INT, o -> o);
			transformByDataType.put(DataTypeDefinition.LONG, o -> (int) o);
			transformByDataType.put(DataTypeDefinition.FLOAT, o -> o);
			transformByDataType.put(DataTypeDefinition.DOUBLE, o -> (float) o);
			transformByDataType.put(DataTypeDefinition.DATE, o -> new DateQL((Date) o));
			transformByDataType.put(DataTypeDefinition.DATETIME, o -> new DateQL((Date) o));
			transformByDataType.put(DataTypeDefinition.BOOLEAN, o -> o);
			transformByDataType.put(DataTypeDefinition.QNAME, o -> ((QName) o).toPrefixString(namespaceService));
			transformByDataType.put(DataTypeDefinition.CATEGORY, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.NODE_REF, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.CHILD_ASSOC_REF, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.ASSOC_REF, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.PATH, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.LOCALE, o -> o.toString());
			transformByDataType.put(DataTypeDefinition.PERIOD, o -> o.toString());
			
			super.init();
		} catch (RuntimeException ex) {
			log.error("GraphQL Init", ex);
		}
	}
	
	@Override
	protected GraphQLConfiguration getConfiguration() {
		try {
			String schemaString = IOUtils.resourceToString(ALFRESCO_SCHEMA, Charset.defaultCharset());

			// Generate field for all types and aspects
			Collection<QName> classes = new TreeSet<>();
			classes.addAll(dictionaryService.getAllTypes());
			classes.addAll(dictionaryService.getAllAspects());

			Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring()
					.type("Query", builder -> builder
						.dataFetcher("node", new StaticDataFetcher(query.getNode()))
						.dataFetcher("authority", new StaticDataFetcher(query.getAuthority()))
					);

			StringBuilder buf = new StringBuilder(schemaString);
			buf.append("\n\ntype PropertiesType {\n");
			runtimeWiringBuilder.type("PropertiesType", builder -> {
				for (QName container : classes) {
					builder.dataFetcher(toFieldName(container), new DataFetcher<NodeQL>() {
						@Override
						public NodeQL get(DataFetchingEnvironment environment) throws Exception {
							return environment.getSource();
						}
					});
					buf.append("	").append(toFieldName(container)).append(": ").append(toFieldName(container)).append("\n");
				}
				return builder;
			});
			buf.append("}\n\n");
			
			for (QName container : classes) {
				buf.append("type ").append(toFieldName(container)).append(" {\n");

				runtimeWiringBuilder.type(toFieldName(container), builder -> {
					for (Entry<QName, PropertyDefinition> entry : dictionaryService.getClass(container).getProperties().entrySet()) {
						QName property = entry.getKey();
						PropertyDefinition def = entry.getValue();
						QName dataType = def.getDataType().getName();
						String type = typeByDataType.getOrDefault(dataType, "String");
						
						buf.append("	").append(toFieldName(property)).append(": ")
							.append(def.isMultiValued() ? "[" : "")
							.append(type)
							.append(def.isMultiValued() ? "]" : "")
							.append("\n");
						
						if (DataTypeDefinition.CONTENT.equals(dataType)) {
							builder.dataFetcher(toFieldName(property), new DataFetcher<Optional<ContentReaderQL>>() {
								@Override
								public Optional<ContentReaderQL> get(DataFetchingEnvironment environment) throws Exception {
									NodeQL node = environment.getSource();	
									return node.getContent(property);
								}
							});
						} else {
							builder.dataFetcher(toFieldName(property), new DataFetcher<Optional<Object>>() {
								@Override
								public Optional<Object> get(DataFetchingEnvironment environment) throws Exception {
									NodeQL node = environment.getSource();
									
									Serializable value = node.getPropertyValue(property);
									if (value == null) return Optional.empty();
									
									Function<Serializable, Object> function = transformByDataType.get(dataType);
									return Optional.of(function.apply(value));
								}
							});
						}
					}
					return builder;
				});

				buf.append("}\n\n");
			}
			RuntimeWiring runtimeWiring = runtimeWiringBuilder.build();
			
			// Generate graphql configuration
			SchemaParser schemaParser = new SchemaParser();
			TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(buf.toString());
			SchemaGenerator schemaGenerator = new SchemaGenerator();
			GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

			return GraphQLConfiguration.with(graphQLSchema).build();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private String toFieldName(QName qname) {
		return qname.getPrefixString().replace(':', '_').replace('-', '_');
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
		} finally {
			AuthenticationUtil.clearCurrentSecurityContext();
		}
	}
}