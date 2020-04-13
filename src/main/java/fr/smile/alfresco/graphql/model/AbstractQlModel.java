package fr.smile.alfresco.graphql.model;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

public abstract class AbstractQlModel {

	private ServiceRegistry serviceRegistry;

	protected AbstractQlModel(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	protected ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
	protected NodeService getNodeService() {
		return serviceRegistry.getNodeService();
	}
	protected NodeQl newNode(NodeRef nodeRef) {
		return new NodeQl(serviceRegistry, nodeRef);
	}
	@SuppressWarnings("unchecked")
	protected <T> T getProperty(NodeRef nodeRef, QName propertyName) {
		return (T) getNodeService().getProperty(nodeRef, propertyName);
	}
}