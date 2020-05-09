package fr.smile.alfresco.graphql.query;

import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.workflow.WorkflowQueryQL;

public class QueryQL extends AbstractQLModel {

	private NodeQueryQL node;
	private AuthorityQueryQL authority;
	private WorkflowQueryQL workflow;
	private SystemQueryQL system;
	
	public QueryQL(QueryContext queryContext) {
		super(queryContext);
		
		this.node = new NodeQueryQL(queryContext);
		this.authority = new AuthorityQueryQL(queryContext);
		this.workflow = new WorkflowQueryQL(queryContext);
		this.system = new SystemQueryQL(queryContext);
	}
	
	public NodeQueryQL getNode() {
		return node;
	}
	public AuthorityQueryQL getAuthority() {
		return authority;
	}
	public WorkflowQueryQL getWorkflow() {
		return workflow;
	}
	public SystemQueryQL getSystem() {
		return system;
	}
}