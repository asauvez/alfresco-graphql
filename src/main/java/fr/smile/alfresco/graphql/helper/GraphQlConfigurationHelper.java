package fr.smile.alfresco.graphql.helper;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;

import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;

import fr.smile.alfresco.graphql.query.ContentReaderQL;
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
	
	private QueryContext queryContext;
	
	private static Map<String, QName> qnameByFieldName = new HashMap<>();
	public static NamespacePrefixResolver namespaceService;
	

	public GraphQlConfigurationHelper(QueryContext queryContext) {
		this.queryContext = queryContext;
		namespaceService = queryContext.getNamespaceService();
	}
	
	public GraphQLConfiguration getConfiguration() {
		try {
			String schemaString = IOUtils.resourceToString(ALFRESCO_SCHEMA, StandardCharsets.UTF_8);
			DictionaryService dictionaryService = queryContext.getServiceRegistry().getDictionaryService();
			
			// Generate field for all types and aspects
			Collection<QName> classes = new TreeSet<>();
			classes.addAll(dictionaryService.getAllTypes());
			classes.addAll(dictionaryService.getAllAspects());

			QueryQL query = new QueryQL(queryContext);
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
			Map<ScalarType, List<QName>> propertiesByType = new HashMap<>();
			
			for (QName container : classes) {
				buf.append("type ").append(toFieldName(container)).append(" {\n");

				runtimeWiringBuilder.type(toFieldName(container), builder -> {
					for (Entry<QName, PropertyDefinition> entry : dictionaryService.getClass(container).getProperties().entrySet()) {
						QName property = entry.getKey();
						PropertyDefinition def = entry.getValue();
						QName dataType = def.getDataType().getName();
						AlfrescoDataType alfrescoDataType = AlfrescoDataType.getForAlfrescoDataType(dataType);
						ScalarType scalarType = alfrescoDataType.getScalarType();
						
						allProperties.add(property);
						List<QName> propertiesForType = propertiesByType.get(scalarType);
						if (propertiesForType == null) {
							propertiesByType.put(scalarType, propertiesForType = new ArrayList<>());
						}
						propertiesForType.add(property);
						if (   def.isIndexed() 
							&& def.getIndexTokenisationMode() != IndexTokenisationMode.FALSE
							&& (scalarType == ScalarType.String || scalarType == ScalarType.ContentData)) {
							tokenizedProperties.add(property);
						}
						
						String fullType = (def.isMultiValued() ? "[" : "") + scalarType.name() + (def.isMultiValued() ? "]" : "");
						String fullInput = (def.isMultiValued() ? "[" : "") + alfrescoDataType.getScalarInput().name() + (def.isMultiValued() ? "]" : "");
						
						if (DataTypeDefinition.CONTENT.equals(dataType)) {
							builder.dataFetcher(toFieldName(property), new DataFetcher<Optional<ContentReaderQL>>() {
								@Override
								public Optional<ContentReaderQL> get(DataFetchingEnvironment env) throws Exception {
									buf.append("	").append(toFieldName(property))
										.append(" : ").append(fullType).append("\n");

									NodeQL node = env.getSource();
									return node.getContent(property);
								}
							});
						} else {
							builder.dataFetcher(toFieldName(property), new DataFetcher<Optional<Object>>() {
								@Override
								public Optional<Object> get(DataFetchingEnvironment env) throws Exception {
									buf.append("	").append(toFieldName(property))
										.append(" (newValue: ").append(fullInput)
										.append(") : ").append(fullType).append("\n");

									NodeQL node = env.getSource();

									Serializable newValue = env.getArgument("newValue");
									if (newValue != null) {
										node.setPropertyValue(property, newValue);
									}
									
									Serializable value = node.getPropertyValue(property);
									return Optional.ofNullable(value)
											.map(alfrescoDataType::toGraphQl);
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
			enumQName(buf, "BooleanPropertyEnum", propertiesByType.get(ScalarType.Boolean));
			enumQName(buf, "IntPropertyEnum", propertiesByType.get(ScalarType.Int));
			
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