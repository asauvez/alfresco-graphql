package fr.smile.alfresco.graphql.model;

import java.util.Optional;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;

import fr.smile.alfresco.graphql.helper.GraphQlConfigurationHelper;

public abstract class AbstractQLModel {

	private ServiceRegistry serviceRegistry;

	protected AbstractQLModel(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	protected ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
	protected NodeService getNodeService() {
		return serviceRegistry.getNodeService();
	}
	protected NamespacePrefixResolver getNamespaceService() {
		return serviceRegistry.getNamespaceService();
	}
	protected AuthorityService getAuthorityService() {
		return serviceRegistry.getAuthorityService();
	}
	protected PermissionService getPermissionService() {
		return serviceRegistry.getPermissionService();
	}
	protected ContentService getContentService() {
		return serviceRegistry.getContentService();
	}

	protected NodeQL newNode(NodeRef nodeRef) {
		return new NodeQL(serviceRegistry, nodeRef);
	}
	protected AuthorityQL newAuthority(String name) {
		return new AuthorityQL(serviceRegistry, name);
	}
	protected AccessPermissionQL newAccessPermission(AccessPermission accessPermission) {
		return new AccessPermissionQL(serviceRegistry, accessPermission);
	}

	@SuppressWarnings("unchecked")
	protected <T> Optional<T> getProperty(NodeRef nodeRef, QName propertyName) {
		return Optional.ofNullable((T) getNodeService().getProperty(nodeRef, propertyName));
	}
	protected Optional<String> getPropertyString(NodeRef nodeRef, QName propertyName) {
		return Optional.ofNullable((String) getNodeService().getProperty(nodeRef, propertyName));
	}
	protected QName getQName(String name) {
		return GraphQlConfigurationHelper.getQName(name);
	}
	protected QNamePattern getQNameFilter(String name) {
		if (name == null) {
			return RegexQNamePattern.MATCH_ALL;
		} else {
			return getQName(name);
		}
	}
}