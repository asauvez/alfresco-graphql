package fr.smile.alfresco.graphql.model;

import org.alfresco.service.ServiceRegistry;

public abstract class AbstractQlModel {

	private ServiceRegistry serviceRegistry;

	public AbstractQlModel(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
}