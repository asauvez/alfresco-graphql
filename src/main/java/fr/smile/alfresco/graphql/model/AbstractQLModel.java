package fr.smile.alfresco.graphql.model;

import java.util.Optional;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;

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
	protected AuthorityService getAuthorityService() {
		return getServiceRegistry().getAuthorityService();
	}
	protected PermissionService getPermissionService() {
		return getServiceRegistry().getPermissionService();
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
		if (name.startsWith(String.valueOf(QName.NAMESPACE_BEGIN))) {
			return QName.createQName(name);
		} else {
			return QName.createQName(name, getServiceRegistry().getNamespaceService());
		}
	}
	protected QNamePattern getQNameFilter(String name) {
		if (name == null) {
			return RegexQNamePattern.MATCH_ALL;
		} else {
			return getQName(name);
		}
	}
}