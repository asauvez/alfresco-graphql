package fr.smile.alfresco.graphql.model;

import org.alfresco.service.ServiceRegistry;

public class QueryQL extends AbstractQLModel {

	private NodeQueryQL node;
	
	public QueryQL(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
		
		this.node = new NodeQueryQL(serviceRegistry);
	}
	
	public NodeQueryQL getNode() {
		return node;
	}
}