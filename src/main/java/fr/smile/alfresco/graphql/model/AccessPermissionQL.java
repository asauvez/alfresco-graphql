package fr.smile.alfresco.graphql.model;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;

public class AccessPermissionQL extends AbstractQLModel {

	private AccessPermission accessPermission;

	public AccessPermissionQL(ServiceRegistry serviceRegistry, AccessPermission accessPermission) {
		super(serviceRegistry);
		this.accessPermission = accessPermission;
	}

	public String getPermission() {
		return accessPermission.getPermission();
	};

	public boolean isAllowed() {
		return (accessPermission.getAccessStatus() == AccessStatus.ALLOWED);
	}
	public boolean isDenied() {
		return (accessPermission.getAccessStatus() == AccessStatus.DENIED);
	}

	public AuthorityQL getAuthority() {
		return newAuthority(accessPermission.getAuthority());
	}

	public AuthorityType getAuthorityType() {
		return accessPermission.getAuthorityType();
	}
	
	public boolean isInherited() {
		return accessPermission.isInherited();
	}

	public boolean isSetDirectly() {
		return accessPermission.isSetDirectly();
	}
}