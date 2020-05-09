package fr.smile.alfresco.graphql.workflow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowService;

import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.query.AbstractQLModel;
import graphql.schema.DataFetchingEnvironment;

public class WorkflowQueryQL extends AbstractQLModel {

	private WorkflowService workflowService;

	public WorkflowQueryQL(QueryContext queryContext) {
		super(queryContext);
		workflowService = queryContext.getWorkflowService();
	}
	
	public WorkflowQueryQL getStart() {
		return this;
	}
	
	public List<WorkflowInstanceQL> getActiveWorkflows() {
		return workflowService.getActiveWorkflows().stream()
				.map(wf -> new WorkflowInstanceQL(getQueryContext(), wf))
				.collect(Collectors.toList());
	}
	public List<WorkflowInstanceQL> getCompletedWorkflows() {
		return workflowService.getCompletedWorkflows().stream()
				.map(wf -> new WorkflowInstanceQL(getQueryContext(), wf))
				.collect(Collectors.toList());
	}
	public List<WorkflowInstanceQL> getWorkflows() {
		return workflowService.getWorkflows().stream()
				.map(wf -> new WorkflowInstanceQL(getQueryContext(), wf))
				.collect(Collectors.toList());
	}
	public Optional<WorkflowInstanceQL> getWorkflowById(DataFetchingEnvironment env) {
		String id = env.getArgument("id");
		return Optional.ofNullable(workflowService.getWorkflowById(id))
				.map(wf -> new WorkflowInstanceQL(getQueryContext(), wf));
	}

	public Optional<WorkflowTaskQL> getTaskById(DataFetchingEnvironment env) {
		String id = env.getArgument("id");
		return Optional.ofNullable(workflowService.getTaskById(id))
				.map(t -> new WorkflowTaskQL(getQueryContext(), t));
	}

	public List<WorkflowDefinition> getDefinitions() {
		return workflowService.getDefinitions();
	}
}