package fr.smile.alfresco.graphql.model;

import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.repo.nodelocator.SharedHomeNodeLocator;
import org.alfresco.repo.nodelocator.SitesHomeNodeLocator;
import org.alfresco.repo.nodelocator.UserHomeNodeLocator;
import org.alfresco.service.ServiceRegistry;

public class NodeQueryQl extends AbstractQlModel {

	public NodeQueryQl(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
	}

	public NodeQl getCompanyHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(CompanyHomeNodeLocator.NAME, null, null));
	}
	public NodeQl getUserHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(UserHomeNodeLocator.NAME, null, null));
	}
	public NodeQl getSharedHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(SharedHomeNodeLocator.NAME, null, null));
	}
	public NodeQl getSitesHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(SitesHomeNodeLocator.NAME, null, null));
	}
}