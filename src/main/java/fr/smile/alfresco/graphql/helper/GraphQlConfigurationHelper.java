package fr.smile.alfresco.graphql.helper;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

import javax.servlet.ServletContext;

import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import fr.smile.alfresco.graphql.query.ContentReaderQL;
import fr.smile.alfresco.graphql.query.DateQL;
import fr.smile.alfresco.graphql.query.NodeQL;
import fr.smile.alfresco.graphql.query.QueryQL;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class GraphQlConfigurationHelper {
	
	private static final String ALFRESCO_SCHEMA = "/alfresco/module/graphql/alfresco.graphqls";
	
	private QueryQL query;
	private DictionaryService dictionaryService;

	private Map<QName, GraphQlType> typeByDataType = new HashMap<>();
	private Map<QName, Function<Serializable, Object>> transformByDataType = new HashMap<>();
	
	private static Map<String, QName> qnameByFieldName = new HashMap<>();
	public static NamespacePrefixResolver namespaceService;
	
	private enum GraphQlType { String, ID, Boolean, Int, Float, Date, ContentData}
	
	public GraphQlConfigurationHelper(ServletContext context) {
		WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(context);
		ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
		QueryContext queryContext = new QueryContext(serviceRegistry);
		query = new QueryQL(queryContext);
		
		dictionaryService = serviceRegistry.getDictionaryService();
		namespaceService = queryContext.getNamespaceService();

		typeByDataType.put(DataTypeDefinition.TEXT, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.ANY, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.ENCRYPTED, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.MLTEXT, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.CONTENT, GraphQlType.ContentData);
		typeByDataType.put(DataTypeDefinition.INT, GraphQlType.Int);
		typeByDataType.put(DataTypeDefinition.LONG, GraphQlType.Int);
		typeByDataType.put(DataTypeDefinition.FLOAT, GraphQlType.Float);
		typeByDataType.put(DataTypeDefinition.DOUBLE, GraphQlType.Float);
		typeByDataType.put(DataTypeDefinition.DATE, GraphQlType.Date);
		typeByDataType.put(DataTypeDefinition.DATETIME, GraphQlType.Date);
		typeByDataType.put(DataTypeDefinition.BOOLEAN, GraphQlType.Boolean);
		typeByDataType.put(DataTypeDefinition.QNAME, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.CATEGORY, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.NODE_REF, GraphQlType.ID);
		typeByDataType.put(DataTypeDefinition.CHILD_ASSOC_REF, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.ASSOC_REF, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.PATH, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.LOCALE, GraphQlType.String);
		typeByDataType.put(DataTypeDefinition.PERIOD, GraphQlType.String);
		
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
	}
	
	public GraphQLConfiguration getConfiguration() {
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
						.dataFetcher("system", new StaticDataFetcher(query.getSystem()))
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
			
			List<QName> allProperties = new ArrayList<>();
			List<QName> tokenizedProperties = new ArrayList<>();
			Map<GraphQlType, List<QName>> propertiesByType = new HashMap<>();
			
			for (QName container : classes) {
				buf.append("type ").append(toFieldName(container)).append(" {\n");

				runtimeWiringBuilder.type(toFieldName(container), builder -> {
					for (Entry<QName, PropertyDefinition> entry : dictionaryService.getClass(container).getProperties().entrySet()) {
						QName property = entry.getKey();
						PropertyDefinition def = entry.getValue();
						QName dataType = def.getDataType().getName();
						GraphQlType type = typeByDataType.getOrDefault(dataType, GraphQlType.String);
						
						allProperties.add(property);
						List<QName> propertiesForType = propertiesByType.get(type);
						if (propertiesForType == null) {
							propertiesByType.put(type, propertiesForType = new ArrayList<>());
						}
						propertiesForType.add(property);
						if (   def.isIndexed() 
							&& def.getIndexTokenisationMode() != IndexTokenisationMode.FALSE
							&& (type == GraphQlType.String || type == GraphQlType.ContentData)) {
							tokenizedProperties.add(property);
						}
						
						buf.append("	").append(toFieldName(property)).append(": ")
							.append(def.isMultiValued() ? "[" : "")
							.append(type.name())
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
			enumQName(buf, "TypeEnum", dictionaryService.getAllTypes());
			enumQName(buf, "AspectEnum", dictionaryService.getAllAspects());
			enumQName(buf, "PropertyEnum", allProperties);
			enumQName(buf, "TokenizePropertyEnum", tokenizedProperties);
			enumQName(buf, "BooleanPropertyEnum", propertiesByType.get(GraphQlType.Boolean));
			enumQName(buf, "IntPropertyEnum", propertiesByType.get(GraphQlType.Int));
			
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
		String fieldName = qname.getPrefixString().replace(':', '_').replace('-', '_');
		qnameByFieldName.put(fieldName, qname);
		return fieldName;
	}
	
	public static QName getQName(String name) {
		QName qName = qnameByFieldName.get(name);
		if (qName != null) return qName;
		
		if (name.startsWith(String.valueOf(QName.NAMESPACE_BEGIN))) {
			return QName.createQName(name);
		} else {
			return QName.createQName(name, namespaceService);
		}
	}
	
	private void enumQName(StringBuilder buf, String typeName, Collection<QName> values) {
		buf.append("enum ").append(typeName).append(" {");
		for (QName value : new TreeSet<QName>(values)) {
			buf.append(toFieldName(value)).append(", ");
		}
		buf.append("}\n");
	}
}