package fr.smile.alfresco.graphql.model;

import org.alfresco.service.ServiceRegistry;

public class QueryQl extends AbstractQlModel {

	private NodeQueryQl node;
	
	public QueryQl(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
		
		this.node = new NodeQueryQl(serviceRegistry);
	}
	
	public NodeQueryQl getNode() {
		return node;
	}
}