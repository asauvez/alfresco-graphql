package fr.smile.alfresco.graphql.workflow;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskDefinition;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;
import org.alfresco.service.namespace.QName;

import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.query.AbstractQLModel;
import fr.smile.alfresco.graphql.query.NodeQL.PropertyValue;
import graphql.schema.DataFetchingEnvironment;

public class WorkflowTaskQL extends AbstractQLModel {

	private WorkflowTask workflowTask;

	public WorkflowTaskQL(QueryContext queryContext, WorkflowTask workflowTask) {
		super(queryContext);
		this.workflowTask = workflowTask;
	}

	public String getId() {
		return workflowTask.getId();
	}
	public String getName() {
		return workflowTask.getName();
	}
	public String getTitle() {
		return workflowTask.getTitle();
	}
	public String getDescription() {
		return workflowTask.getDescription();
	}
	public WorkflowTaskState getState() {
		return workflowTask.getState();
	}
	public WorkflowPathQL getPath() {
		return new WorkflowPathQL(getQueryContext(), workflowTask.getPath());
	}
	public WorkflowTaskDefinition getDefinition() {
		return workflowTask.getDefinition();
	}

	public List<PropertyValue> getPropertiesList() {
		return workflowTask.getProperties().entrySet().stream()
				.map(entry -> new PropertyValue(
						entry.getKey().toPrefixString(getNamespaceService()), 
						Optional.ofNullable(entry.getValue()).map(Serializable::toString)))
				.collect(Collectors.toList());
	}
	public Optional<String> getPropertyAsString(DataFetchingEnvironment env) {
		QName propertyName = getQName(env.getArgument("name"));
		return Optional.ofNullable(workflowTask.getProperties().get(propertyName))
				.map(Object::toString);
	}
}