package fr.smile.alfresco.graphql.model;

import org.alfresco.service.ServiceRegistry;

public class QueryQL extends AbstractQLModel {

	private NodeQueryQL node;
	private AuthorityQueryQL authority;
	private SystemQueryQL system;
	
	public QueryQL(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
		
		this.node = new NodeQueryQL(serviceRegistry);
		this.authority = new AuthorityQueryQL(serviceRegistry);
		this.system = new SystemQueryQL(serviceRegistry);
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