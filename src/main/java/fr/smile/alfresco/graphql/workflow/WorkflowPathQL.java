package fr.smile.alfresco.graphql.workflow;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.workflow.WorkflowNode;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.namespace.QName;

import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.query.AbstractQLModel;
import fr.smile.alfresco.graphql.query.NodeQL.PropertyValue;
import graphql.schema.DataFetchingEnvironment;

public class WorkflowPathQL extends AbstractQLModel {

	private WorkflowPath workflowPath;

	public WorkflowPathQL(QueryContext queryContext, WorkflowPath workflowPath) {
		super(queryContext);
		this.workflowPath = workflowPath;
	}

	public String getId() {
		return workflowPath.getId();
	}
	public WorkflowInstanceQL getInstance() {
		return new WorkflowInstanceQL(getQueryContext(), workflowPath.getInstance());
	}
	public WorkflowNode getNode() {
		return workflowPath.getNode();
	}
	public boolean isActive() {
		return workflowPath.isActive();
	}
	
	public List<WorkflowTaskQL> getTasks() {
		return getWorkflowService().getTasksForWorkflowPath(workflowPath.getId()).stream()
				.map(t -> new WorkflowTaskQL(getQueryContext(), t))
				.collect(Collectors.toList());
	}
	
	public List<PropertyValue> getPropertiesList() {
		return getWorkflowService().getPathProperties(workflowPath.getId()).entrySet().stream()
				.map(entry -> new PropertyValue(
						entry.getKey().toPrefixString(getNamespaceService()), 
						Optional.ofNullable(entry.getValue()).map(Serializable::toString)))
				.collect(Collectors.toList());
	}
	public Optional<String> getPropertyAsString(DataFetchingEnvironment env) {
		QName propertyName = getQName(env.getArgument("name"));
		return Optional.ofNullable(getWorkflowService().getPathProperties(workflowPath.getId()).get(propertyName))
				.map(Object::toString);
	}
	
	public WorkflowPathQL signal(DataFetchingEnvironment env) {
		String transitionId = env.getArgument("transition");
		return new WorkflowPathQL(getQueryContext(), getWorkflowService().signal(workflowPath.getId(), transitionId));
	}
}