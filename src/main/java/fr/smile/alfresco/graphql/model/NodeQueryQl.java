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

	public NodeQl companyHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(CompanyHomeNodeLocator.NAME, null, null));
	}
	public NodeQl userHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(UserHomeNodeLocator.NAME, null, null));
	}
	public NodeQl sharedHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(SharedHomeNodeLocator.NAME, null, null));
	}
	public NodeQl sitesHome() {
		return new NodeQl(getServiceRegistry(), 
				getServiceRegistry().getNodeLocatorService().getNode(SitesHomeNodeLocator.NAME, null, null));
	}
}