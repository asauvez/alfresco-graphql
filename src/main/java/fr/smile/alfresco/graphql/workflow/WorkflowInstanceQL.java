package fr.smile.alfresco.graphql.workflow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowInstance;

import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.query.AbstractQLModel;
import fr.smile.alfresco.graphql.query.AuthorityQL;
import fr.smile.alfresco.graphql.query.DateQL;
import fr.smile.alfresco.graphql.query.NodeQL;

public class WorkflowInstanceQL extends AbstractQLModel {

	private WorkflowInstance workflowInstance;

	public WorkflowInstanceQL(QueryContext queryContext, WorkflowInstance workflowInstance) {
		super(queryContext);
		this.workflowInstance = workflowInstance;
	}

	public String getId() {
		return workflowInstance.getId();
	}
	public String getDescription() {
		return workflowInstance.getDescription();
	}
	public boolean isActive() {
		return workflowInstance.isActive();
	}
	public NodeQL getInitiatorNode() {
		return newNode(workflowInstance.getInitiator());
	}
	public Optional<AuthorityQL> getInitiator() {
		return getProperty(workflowInstance.getInitiator(), ContentModel.PROP_USERNAME)
				.map(username -> newAuthority((String) username));
	}
	public Integer getPriority() {
		return workflowInstance.getPriority();
	}
	public DateQL getStartDate() {
		return newDate(workflowInstance.getStartDate());
	}
	public DateQL getDueDate() {
		return newDate(workflowInstance.getDueDate());
	}
	public DateQL getEndDate() {
		return newDate(workflowInstance.getEndDate());
	}
	public NodeQL getWorkflowPackage() {
		return newNode(workflowInstance.getWorkflowPackage());
	}
	public NodeQL getContext() {
		return newNode(workflowInstance.getContext());
	}
	public WorkflowDefinition getDefinition() {
		return workflowInstance.getDefinition();
	}
	
	public List<WorkflowPathQL> getPaths() {
		return getWorkflowService().getWorkflowPaths(workflowInstance.getId()).stream()
				.map(p -> new WorkflowPathQL(getQueryContext(), p))
				.collect(Collectors.toList());
	}
	public WorkflowTaskQL getStartTask() {
		return new WorkflowTaskQL(getQueryContext(), getWorkflowService().getStartTask(workflowInstance.getId()));
	}
	
	public WorkflowInstanceQL getCancelWorkflow() {
		return new WorkflowInstanceQL(getQueryContext(), getWorkflowService().cancelWorkflow(workflowInstance.getId()));
	}
	public WorkflowInstanceQL deleteWorkflow() {
		return new WorkflowInstanceQL(getQueryContext(), getWorkflowService().deleteWorkflow(workflowInstance.getId()));
	}
}