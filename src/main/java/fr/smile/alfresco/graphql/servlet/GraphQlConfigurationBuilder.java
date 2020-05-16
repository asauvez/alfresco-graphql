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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionDefinition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.AssociationDefinition;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskDefinition;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.smile.alfresco.graphql.helper.AlfrescoDataType;
import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.helper.ScalarType;
import fr.smile.alfresco.graphql.query.ContainerNodeQL;
import fr.smile.alfresco.graphql.query.NodeQL;
import fr.smile.alfresco.graphql.query.QueryQL;
import fr.smile.alfresco.graphql.query.SystemQueryQL;
import fr.smile.alfresco.graphql.workflow.WorkflowPathQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionStrategy;
import graphql.execution.SimpleDataFetcherExceptionHandler;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.config.DefaultExecutionStrategyProvider;
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

	private static Log log = LogFactory.getLog(SystemQueryQL.class);

	private static final String ALFRESCO_SCHEMA = "/alfresco/module/graphql/alfresco.graphqls";
	
	private QueryContext queryContext;
	
	private static Map<String, QName> qnameByFieldName = new HashMap<>();
	public static NamespacePrefixResolver namespaceService;
	private WorkflowService workflowService;
	

	public GraphQlConfigurationBuilder(QueryContext queryContext) {
		this.queryContext = queryContext;
		namespaceService = queryContext.getNamespaceService();
		workflowService = queryContext.getWorkflowService();
	}
	
	public String getSchema(Builder runtimeWiringBuilder) throws IOException {
		String schemaString = IOUtils.resourceToString(ALFRESCO_SCHEMA, StandardCharsets.UTF_8);
		DictionaryService dictionaryService = queryContext.getServiceRegistry().getDictionaryService();
		
		// Generate field for all types and aspects
		Collection<QName> allNodeTypes = new TreeSet<>(dictionaryService.getAllTypes());
		Collection<QName> allNodeAspects = new TreeSet<>(dictionaryService.getAllAspects());

		Collection<QName> wfTypes = new TreeSet<>();
		for (WorkflowDefinition workflowDefinition : workflowService.getAllDefinitions()) {
			for (WorkflowTaskDefinition workflowTaskDefinition : workflowService.getTaskDefinitions(workflowDefinition.getId())) {
				collectWfType(wfTypes, workflowTaskDefinition.getMetadata());
			}
		}
		allNodeTypes.removeAll(wfTypes);
		allNodeAspects.removeAll(wfTypes);
		
		Collection<QName> nodeClasses = new TreeSet<>();
		nodeClasses.addAll(allNodeTypes);
		nodeClasses.addAll(allNodeAspects);

		QueryQL query = new QueryQL(queryContext);
		runtimeWiringBuilder.type("Query", builder -> builder
					.dataFetcher("node", new StaticDataFetcher(query.getNode()))
					.dataFetcher("authority", new StaticDataFetcher(query.getAuthority()))
					.dataFetcher("workflow", new StaticDataFetcher(query.getWorkflow()))
					.dataFetcher("system", new StaticDataFetcher(query.getSystem()))
				);
		
		StringBuilder buf = new StringBuilder(schemaString);

		Map<QName, Set<AssociationDefinition>> sourceAssociationsByType = new HashMap<>();
		buf.append("\n\ntype NodePropertiesType {\n");
		runtimeWiringBuilder.type("NodePropertiesType", builder -> {
			for (QName container : nodeClasses) {
				builder.dataFetcher(toFieldName(container), new DataFetcher<ContainerNodeQL>() {
					@Override
					public ContainerNodeQL get(DataFetchingEnvironment environment) throws Exception {
						NodeQL node = environment.getSource();
						return node.newContainerNode(container);
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
		
		for (QName container : nodeClasses) {
			buf.append("type ").append(toFieldName(container)).append(" {\n");
			if (allNodeTypes.contains(container)) {
				buf.append("	isExactType: Boolean\n");
				buf.append("	isSubType: Boolean\n");
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
					configureNodeField(buf, allProperties, tokenizedProperties, propertiesByType, builder, def);
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
		enumQName(buf, "TypeEnum", allNodeTypes);
		enumQName(buf, "AspectEnum", allNodeAspects);
		enumQName(buf, "PropertyEnum", allProperties);
		enumQName(buf, "TokenizePropertyEnum", tokenizedProperties);
		enumQName(buf, "BooleanPropertyEnum", propertiesByType.get(ScalarType.Boolean));
		enumQName(buf, "IntPropertyEnum", propertiesByType.get(ScalarType.Int));

		generateActions(buf, runtimeWiringBuilder);
		generateStartWorkflow(buf, runtimeWiringBuilder);

		buf.append("\n\ntype WorkflowPropertiesType {\n");
		runtimeWiringBuilder.type("WorkflowPropertiesType", builder -> {
			for (QName container : wfTypes) {
				builder.dataFetcher(toFieldName(container), new DataFetcher<Map<QName, Serializable>>() {
					@Override
					public Map<QName, Serializable> get(DataFetchingEnvironment environment) throws Exception {
						return environment.getSource();
					}
				});
				buf.append("	").append(toFieldName(container)).append(": ").append(toFieldName(container)).append("\n");
			}
			return builder;
		});
		buf.append("}\n\n");

		for (QName container : wfTypes) {
			buf.append("type ").append(toFieldName(container)).append(" {\n");

			runtimeWiringBuilder.type(toFieldName(container), builder -> {
				ClassDefinition classDefinition = dictionaryService.getClass(container);
				Collection<PropertyDefinition> properties = classDefinition.getProperties().values();
				for (PropertyDefinition def : properties) {
					QName dataType = def.getDataType().getName();
					AlfrescoDataType alfrescoDataType = AlfrescoDataType.getForAlfrescoDataType(dataType);
					configureWorkflowField(buf, builder, def.getName(), alfrescoDataType, def.isMultiValued());
				}
				
				Collection<AssociationDefinition> targetAssociations = classDefinition.getAssociations().values();
				for (AssociationDefinition def : targetAssociations) {
					configureWorkflowField(buf, builder, def.getName(), AlfrescoDataType.NODE_REF, def.isTargetMany());
				}

				return builder;
			});

			buf.append("}\n\n");
		}

		return buf.toString();
	}

	private void configureWorkflowField(StringBuilder buf, graphql.schema.idl.TypeRuntimeWiring.Builder builder,
			QName property, AlfrescoDataType alfrescoDataType, boolean isMultiValued) {
		ScalarType scalarType = alfrescoDataType.getScalarType();
		
		String fullType = (isMultiValued ? "[" : "") + scalarType.name() + (isMultiValued ? "]" : "");
		buf.append("	").append(toFieldName(property));
		buf.append(": ").append(fullType).append("\n");
		
		builder.dataFetcher(toFieldName(property), new DataFetcher<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object get(DataFetchingEnvironment env) throws Exception {
				Map<QName, Serializable> properties = env.getSource();
				
				Serializable value = properties.get(property);
				Function<Serializable, Object> function = (item) -> alfrescoDataType.toGraphQl(queryContext, null, property, item);
				return (value instanceof List) 
						? ((List<Serializable>) value).stream().map(function).collect(Collectors.toList())
						: Optional.ofNullable(value).map(function);
			}
		});
	}
	
	private void collectWfType(Collection<QName> wfTypes, ClassDefinition classDefinition) {
		if (   classDefinition != null
			&& !NamespaceService.CONTENT_MODEL_1_0_URI.equals(classDefinition.getName().getNamespaceURI()) 
			&& !NamespaceService.SYSTEM_MODEL_1_0_URI.equals(classDefinition.getName().getNamespaceURI())) {
			wfTypes.add(classDefinition.getName());
			
			collectWfType(wfTypes, classDefinition.getParentClassDefinition());
			for (AspectDefinition aspect : classDefinition.getDefaultAspects()) {
				collectWfType(wfTypes, aspect);
			}
		}
	}
	
	public GraphQLConfiguration getConfiguration() throws IOException {
		Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
		String schema = getSchema(runtimeWiringBuilder);
				
		SchemaParser schemaParser = new SchemaParser();
		TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

		RuntimeWiring runtimeWiring = runtimeWiringBuilder.build();
		SchemaGenerator schemaGenerator = new SchemaGenerator();
		GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

		SimpleDataFetcherExceptionHandler exceptionHandler = new SimpleDataFetcherExceptionHandler() {
			@Override
			public DataFetcherExceptionHandlerResult onException(
					DataFetcherExceptionHandlerParameters handlerParameters) {

				Throwable retryCause = RetryingTransactionHelper.extractRetryCause(handlerParameters.getException());
				if (retryCause != null) {
					queryContext.setRetryException(retryCause);
				} else {
					log.error(handlerParameters.getPath(), handlerParameters.getException());
				}
				
				return super.onException(handlerParameters);
			}
		};
		ExecutionStrategy executionStrategy = new AsyncExecutionStrategy(exceptionHandler);
		GraphQLQueryInvoker invoker = GraphQLQueryInvoker.newBuilder()
				.withExecutionStrategyProvider(new DefaultExecutionStrategyProvider(executionStrategy))
				.build();
		
		return GraphQLConfiguration
				.with(graphQLSchema)
				.with(invoker)
				.build();
	}

	private void configureNodeField(StringBuilder buf, List<QName> allProperties, List<QName> tokenizedProperties,
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
			buf.append(" (setValue: ").append(fullInput).append(", remove: Boolean = false");
			if (def.isMultiValued()) {
				buf.append(", append: ").append(alfrescoDataType.getScalarInput().name());
			} else if (alfrescoDataType == AlfrescoDataType.INT || alfrescoDataType == AlfrescoDataType.LONG) {
				buf.append(", increment: ").append(alfrescoDataType.getScalarInput().name());
			}
			buf.append(")");
		}
		buf.append(": ").append(fullType).append("\n");
		
		builder.dataFetcher(toFieldName(property), new DataFetcher<Object>() {
			@Override
			public Object get(DataFetchingEnvironment env) throws Exception {
				ContainerNodeQL cnode = env.getSource();
				return cnode.getPropertyValue(env, property, alfrescoDataType);
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
						.getChildAssocs(cnode.getNodeRef(), assocType, RegexQNamePattern.MATCH_ALL)
						.stream().map(assoc -> assoc.getChildRef()) 
					: queryContext.getNodeService()
						.getTargetAssocs(cnode.getNodeRef(), assocType)
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
						.getParentAssocs(cnode.getNodeRef(), assocType, RegexQNamePattern.MATCH_ALL)
						.stream().map(assoc -> assoc.getParentRef()) 
					: queryContext.getNodeService()
						.getSourceAssocs(cnode.getNodeRef(), assocType)
						.stream().map(assoc -> assoc.getSourceRef()))
					.map(n -> new NodeQL(queryContext, n));
				return def.isSourceMany()
					? nodes.sorted().collect(Collectors.toList())
					: nodes.findFirst();
			}
		});
	}

	private String toFieldName(QName qname) {
		String fieldName = toFieldName(qname.getPrefixString());
		qnameByFieldName.put(fieldName, qname);
		return fieldName;
	}
	private String toFieldName(String name) {
		return name.replace(':', '_').replace('-', '_').replace('$', '_');
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
		enumString(buf, typeName, values.stream()
				.map(this::toFieldName).collect(Collectors.toList()));
	}
	private void enumString(StringBuilder buf, String typeName, Collection<String> values) {
		buf.append("\nenum ").append(typeName).append(" {");
		for (String value : new TreeSet<String>(values)) {
			buf.append(value).append(", ");
		}
		buf.append("}\n");
	}
	
	private void generateActions(StringBuilder buf, Builder runtimeWiringBuilder) {
		buf.append("\ntype Actions {\n");

		runtimeWiringBuilder.type("Actions", builder -> {
			ActionService actionService = queryContext.getActionService();
			List<ActionDefinition> actionDefinitions = actionService.getActionDefinitions();
			for (ActionDefinition actionDefinition : actionDefinitions) {
				String actionName = toFieldName(actionDefinition.getName());
				buf.append("	").append(actionName).append("(");
				List<ParameterDefinition> parameterDefinitions = actionDefinition.getParameterDefinitions();
				for (ParameterDefinition parameterDefinition : parameterDefinitions) {
					AlfrescoDataType alfrescoDataType = AlfrescoDataType.getForAlfrescoDataType(parameterDefinition.getType());
					String fullInput = (parameterDefinition.isMultiValued() ? "[" : "") + alfrescoDataType.getScalarInput().name() + (parameterDefinition.isMultiValued() ? "!]" : "");
					
					buf.append(toFieldName(parameterDefinition.getName()))
						.append(":").append(fullInput)
						.append(parameterDefinition.isMandatory() ? "!" : "")
						.append(", ");
				}
				buf.append("executeAsynchronously: Boolean! = false) : Boolean!\n");
				
				builder.dataFetcher(actionName, new DataFetcher<Boolean>() {
					@Override
					public Boolean get(DataFetchingEnvironment env) throws Exception {
						NodeQL node = env.getSource();
						boolean executeAsynchronously = env.getArgument("executeAsynchronously");
						
						Map<String, Serializable> params = new HashMap<>();
						for (ParameterDefinition parameterDefinition : parameterDefinitions) {
							Serializable value = env.getArgument(toFieldName(parameterDefinition.getName()));
							params.put(parameterDefinition.getName(), value);
						}
						
						Action action = actionService.createAction(actionDefinition.getName(), params);
						actionService.executeAction(action, node.getNodeRefInternal(), true, executeAsynchronously);
						
						return Boolean.TRUE;
					}
				});
			}
			buf.append("}\n");
			return builder;
		});
	}


	private void generateStartWorkflow(StringBuilder buf, Builder runtimeWiringBuilder) {
		buf.append("\ntype StartWorkflow {\n");

		runtimeWiringBuilder.type("StartWorkflow", builder -> {
			List<WorkflowDefinition> workflowDefinitions = workflowService.getDefinitions();
			for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
				String actionName = toFieldName(workflowDefinition.getName());
				buf.append("	").append(actionName).append("(\n");
				TypeDefinition metadata = workflowDefinition.getStartTaskDefinition().getMetadata();
				
				Map<QName, PropertyDefinition> propertyDefinitions = new TreeMap<>(metadata.getProperties());
				Map<QName, AssociationDefinition> assocDefinitions = new TreeMap<>(metadata.getAssociations());
				for (AspectDefinition aspect : metadata.getDefaultAspects()) {
					if (   !NamespaceService.CONTENT_MODEL_1_0_URI.equals(aspect.getName().getNamespaceURI()) 
						&& !NamespaceService.SYSTEM_MODEL_1_0_URI.equals(aspect.getName().getNamespaceURI())) {
						propertyDefinitions.putAll(aspect.getProperties());
						assocDefinitions.putAll(aspect.getAssociations());
					}
				}
				
				for (PropertyDefinition propertyDefinition : propertyDefinitions.values()) {
					if (propertyDefinition.getName().equals(WorkflowModel.PROP_TASK_ID)) {
						continue;
					}
					
					AlfrescoDataType alfrescoDataType = AlfrescoDataType.getForAlfrescoDataType(propertyDefinition.getDataType().getName());
					String fullInput = (propertyDefinition.isMultiValued() ? "[" : "") + alfrescoDataType.getScalarInput().name() + (propertyDefinition.isMultiValued() ? "!]" : "");
					String defaultValue = propertyDefinition.getDefaultValue();
					if (defaultValue != null) {
						if (alfrescoDataType.getScalarInput() == ScalarType.String) {
							defaultValue = "\"" + defaultValue.replace("\"", "\\\"") + "\"";
						}
						if (propertyDefinition.isMultiValued()) {
							defaultValue = "[" + defaultValue + "]";
						}
					}
					
					buf.append("		").append(toFieldName(propertyDefinition.getName()))
						.append(":").append(fullInput)
						.append(propertyDefinition.isMandatory() ? "!" : "")
						.append(defaultValue != null ? " = " + defaultValue : "")
						.append(",\n");
				}
				
				for (AssociationDefinition assoc : assocDefinitions.values()) {
					if (assoc.getName().equals(WorkflowModel.ASSOC_PACKAGE)) {
						continue;
					}
					buf.append("		").append(toFieldName(assoc.getName()))
						.append(":").append(assoc.isTargetMany() ? "[" : "")
						.append("ID")
						.append(assoc.isTargetMany() ? "]" : "")
						.append(",\n");
				}
				
				buf.append(") : WorkflowPath!\n");
				
				builder.dataFetcher(actionName, new DataFetcher<WorkflowPathQL>() {
					@SuppressWarnings("unchecked")
					@Override
					public WorkflowPathQL get(DataFetchingEnvironment env) throws Exception {
						Map<QName, Serializable> parameters = new HashMap<>();

						for (PropertyDefinition propertyDefinition : propertyDefinitions.values()) {
							Serializable value = env.getArgument(toFieldName(propertyDefinition.getName()));
							parameters.put(propertyDefinition.getName(), value);
						}
						for (AssociationDefinition assoc : assocDefinitions.values()) {
							Serializable value = env.getArgument(toFieldName(assoc.getName()));
							
							if (value instanceof List) {
								value = (Serializable) ((List<String>) value).stream()
									.map(v -> queryContext.getQueryVariableAuthority(v))
									.collect(Collectors.toList());
							} else if (value instanceof String) {
								value = queryContext.getQueryVariableAuthority((String) value);
							}
							
							parameters.put(assoc.getName(), value);
						}
						
						Object source = env.getSource();
						if (source instanceof NodeQL) {
							NodeQL node = (NodeQL) source;
							parameters.put(WorkflowModel.ASSOC_PACKAGE, node.getNodeRefInternal());
							parameters.put(WorkflowModel.PROP_CONTEXT, node.getNodeRefInternal());
						}
						
						WorkflowPath workflowPath = workflowService.startWorkflow(workflowDefinition.getId(), parameters);
						List<WorkflowTask> tasks = workflowService.getTasksForWorkflowPath(workflowPath.getId());
						for (WorkflowTask task : tasks) {
							workflowService.endTask(task.getId(), null);
						}
						return new WorkflowPathQL(queryContext, workflowPath);
					}
				});
			}
			buf.append("}\n");
			return builder;
		});
	}
}