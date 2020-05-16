package fr.smile.alfresco.graphql.workflow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTaskQuery;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;

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
	public List<WorkflowTaskQL> getQueryTasks(DataFetchingEnvironment env) {
		WorkflowTaskQuery query = new WorkflowTaskQuery();
		
		// task predicates
		query.setTaskId(env.getArgument("taskId"));
		String taskState = env.getArgument("taskState");
		query.setTaskState((taskState != null) ? WorkflowTaskState.valueOf(taskState) : null);
		//QName taskName;
		query.setActorId(env.getArgument("actorId"));
		//Map<QName, Object> taskCustomProps; 
		
		// process predicates
		query.setProcessId(env.getArgument("processId"));
		//QName processName;
		query.setWorkflowDefinitionName(env.getArgument("workflowDefinitionName"));
		query.setActive(env.getArgument("active"));
		//Map<QName, Object> processCustomProps;
		
		// order by
		//OrderBy[] orderBy;

		query.setLimit(env.getArgument("limit"));
		
		return workflowService.queryTasks(query, true).stream()
				.map(t -> new WorkflowTaskQL(getQueryContext(), t))
				.collect(Collectors.toList());
	}
	
	public List<WorkflowDefinition> getDefinitions() {
		return workflowService.getDefinitions();
	}
}