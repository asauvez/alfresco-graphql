package fr.smile.alfresco.graphql.model;

import fr.smile.alfresco.graphql.helper.QueryContext;

public class QueryQL extends AbstractQLModel {

	private NodeQueryQL node;
	private AuthorityQueryQL authority;
	private SystemQueryQL system;
	
	public QueryQL(QueryContext queryContext) {
		super(queryContext);
		
		this.node = new NodeQueryQL(queryContext);
		this.authority = new AuthorityQueryQL(queryContext);
		this.system = new SystemQueryQL(queryContext);
	}
	
	public NodeQueryQL getNode() {
		return node;
	}
	public AuthorityQueryQL getAuthority() {
		return authority;
	}
	public SystemQueryQL getSystem() {
		return system;
	}
}