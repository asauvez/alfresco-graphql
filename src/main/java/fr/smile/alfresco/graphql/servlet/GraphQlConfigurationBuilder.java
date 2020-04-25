package fr.smile.alfresco.graphql.servlet;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.io.IOUtils;

import fr.smile.alfresco.graphql.helper.AlfrescoDataType;
import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.helper.ScalarType;
import fr.smile.alfresco.graphql.query.ContainerNodeQL;
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
import graphql.schema.idl.TypeRuntimeWiring;

public class GraphQlConfigurationBuilder {
	
	private static final String ALFRESCO_SCHEMA = "/alfresco/module/graphql/alfresco.graphqls";
	
	private QueryContext queryContext;
	
	private static Map<String, QName> qnameByFieldName = new HashMap<>();
	public static NamespacePrefixResolver namespaceService;
	

	public GraphQlConfigurationBuilder(QueryContext queryContext) {
		this.queryContext = queryContext;
		namespaceService = queryContext.getNamespaceService();
	}
	
	public String getSchema(Builder runtimeWiringBuilder) throws IOException {
		String schemaString = IOUtils.resourceToString(ALFRESCO_SCHEMA, StandardCharsets.UTF_8);
		DictionaryService dictionaryService = queryContext.getServiceRegistry().getDictionaryService();
		
		// Generate field for all types and aspects
		Collection<QName> classes = new TreeSet<>();
		Collection<QName> allTypes = new HashSet<>(dictionaryService.getAllTypes());
		classes.addAll(allTypes);
		classes.addAll(dictionaryService.getAllAspects());

		QueryQL query = new QueryQL(queryContext);
		runtimeWiringBuilder.type("Query", builder -> builder
					.dataFetcher("node", new StaticDataFetcher(query.getNode()))
					.dataFetcher("authority", new StaticDataFetcher(query.getAuthority()))
					.dataFetcher("system", new StaticDataFetcher(query.getSystem()))
				);
		
		Map<QName, Set<AssociationDefinition>> sourceAssociationsByType = new HashMap<>();
		StringBuilder buf = new StringBuilder(schemaString);
		buf.append("\n\ntype PropertiesType {\n");
		runtimeWiringBuilder.type("PropertiesType", builder -> {
			for (QName container : classes) {
				builder.dataFetcher(toFieldName(container), new DataFetcher<ContainerNodeQL>() {
					@Override
					public ContainerNodeQL get(DataFetchingEnvironment environment) throws Exception {
						return new ContainerNodeQL(environment.getSource(), container) ;
					}
				});
				buf.append("	").append(toFieldName(container)).append(": ").append(toFieldName(container)).append("\n");
				
				for (AssociationDefinition def : dictionaryService.getClass(container).getAssociations().values()) {
					QName targetType = def.getTargetClass().getName();
					Set<AssociationDefinition> assocs = sourceAssociationsByType.get(targetType);
					if (assocs == null) {
						sourceAssociationsByType.put(targetType, assocs = new HashSet<>());
					}
					assocs.add(def);
				}
			}
			return builder;
		});
		buf.append("}\n\n");
		
		List<QName> allProperties = new ArrayList<>();
		List<QName> tokenizedProperties = new ArrayList<>();
		Map<ScalarType, List<QName>> propertiesByType = new HashMap<>();
		
		for (QName container : classes) {
			buf.append("type ").append(toFieldName(container)).append(" {\n");
			if (allTypes.contains(container)) {
				buf.append("	isType: Boolean\n");
				buf.append("	setType: Boolean\n");
			} else {
				buf.append("	hasAspect: Boolean\n");
				buf.append("	addAspect: Boolean\n");
				buf.append("	removeAspect: Boolean\n");
			}

			runtimeWiringBuilder.type(toFieldName(container), builder -> {
				ClassDefinition classDefinition = dictionaryService.getClass(container);
				Collection<PropertyDefinition> properties = classDefinition.getProperties().values();
				for (PropertyDefinition def : properties) {
					configureField(buf, allProperties, tokenizedProperties, propertiesByType, builder, def);
				}
				
				Collection<AssociationDefinition> targetAssociations = classDefinition.getAssociations().values();
				for (AssociationDefinition def : targetAssociations) {
					configureTargetAssociation(buf, builder, def);
				}

				Set<AssociationDefinition> sourceAssociations = sourceAssociationsByType.getOrDefault(container, Collections.emptySet());
				for (AssociationDefinition def : sourceAssociations) {
					configureSourceAssociation(buf, builder, def);
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
		
		return buf.toString();
	}
		
	public GraphQLConfiguration getConfiguration() throws IOException {
		Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
		String schema = getSchema(runtimeWiringBuilder);
				
		SchemaParser schemaParser = new SchemaParser();
		TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

		RuntimeWiring runtimeWiring = runtimeWiringBuilder.build();
		SchemaGenerator schemaGenerator = new SchemaGenerator();
		GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

		return GraphQLConfiguration.with(graphQLSchema).build();
	}

	private void configureField(StringBuilder buf, List<QName> allProperties, List<QName> tokenizedProperties,
			Map<ScalarType, List<QName>> propertiesByType, TypeRuntimeWiring.Builder builder,
			PropertyDefinition def) {
		QName property = def.getName();
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
		buf.append("	").append(toFieldName(property));
		if (alfrescoDataType != AlfrescoDataType.CONTENT) {
			buf.append(" (setValue: ").append(fullInput).append(")");
		}
		buf.append(": ").append(fullType).append("\n");
		
		builder.dataFetcher(toFieldName(property), new DataFetcher<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object get(DataFetchingEnvironment env) throws Exception {
				ContainerNodeQL cnode = env.getSource();

				Serializable setValue = env.getArgument("setValue");
				if (setValue != null) {
					cnode.getNode().setPropertyValue(property, setValue);
				}
				
				Serializable value = cnode.getNode().getPropertyValue(property);
				Function<Serializable, Object> function = (item) -> alfrescoDataType.toGraphQl(cnode.getNode(), property, item);
				return (value instanceof List) 
						? ((List<Serializable>) value).stream().map(function).collect(Collectors.toList())
						: Optional.ofNullable(value).map(function);
			}
		});
	}

	private void configureTargetAssociation(StringBuilder buf, TypeRuntimeWiring.Builder builder,
			AssociationDefinition def) {
		QName assocType = def.getName();
		String targetFieldName = toFieldName(assocType); // + (def.isChild() ?  "Children" : "Target");
		buf.append("	").append(targetFieldName).append(": ")
			.append(def.isTargetMany() ? "[Node!]!" : "Node").append("\n");
		
		builder.dataFetcher(targetFieldName, new DataFetcher<Object>() {
			@Override
			public Object get(DataFetchingEnvironment env) throws Exception {
				ContainerNodeQL cnode = env.getSource();
				
				Stream<NodeQL> nodes = (def.isChild()
					? queryContext.getNodeService()
						.getChildAssocs(cnode.getNode().getNodeRefInternal(), assocType, RegexQNamePattern.MATCH_ALL)
						.stream().map(assoc -> assoc.getChildRef()) 
					: queryContext.getNodeService()
						.getTargetAssocs(cnode.getNode().getNodeRefInternal(), assocType)
						.stream().map(assoc -> assoc.getTargetRef()))
					.map(n -> new NodeQL(queryContext, n));
				return def.isTargetMany()
					? nodes.collect(Collectors.toList())
					: nodes.findFirst();
			}
		});
	}
	
	private void configureSourceAssociation(StringBuilder buf, TypeRuntimeWiring.Builder builder,
			AssociationDefinition def) {
		QName assocType = def.getName();
		String sourceFieldName = toFieldName(assocType) + (def.isChild() ?  "Parent" : "Source");
		buf.append("	").append(sourceFieldName).append(": ")
			.append(def.isSourceMany() ? "[Node!]!" : "Node").append("\n");
		
		builder.dataFetcher(sourceFieldName, new DataFetcher<Object>() {
			@Override
			public Object get(DataFetchingEnvironment env) throws Exception {
				ContainerNodeQL cnode = env.getSource();
				
				Stream<NodeQL> nodes = (def.isChild()
					? queryContext.getNodeService()
						.getParentAssocs(cnode.getNode().getNodeRefInternal(), assocType, RegexQNamePattern.MATCH_ALL)
						.stream().map(assoc -> assoc.getParentRef()) 
					: queryContext.getNodeService()
						.getSourceAssocs(cnode.getNode().getNodeRefInternal(), assocType)
						.stream().map(assoc -> assoc.getSourceRef()))
					.map(n -> new NodeQL(queryContext, n));
				return def.isSourceMany()
					? nodes.collect(Collectors.toList())
					: nodes.findFirst();
			}
		});
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