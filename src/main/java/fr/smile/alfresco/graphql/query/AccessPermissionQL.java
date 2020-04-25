package fr.smile.alfresco.graphql.query;

import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;

import fr.smile.alfresco.graphql.helper.QueryContext;

public class AccessPermissionQL extends AbstractQLModel implements Comparable<AccessPermissionQL> {

	private AccessPermission accessPermission;

	public AccessPermissionQL(QueryContext queryContext, AccessPermission accessPermission) {
		super(queryContext);
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
	
	@Override
	public int compareTo(AccessPermissionQL o) {
		int diff = this.accessPermission.getAuthority().compareTo(o.accessPermission.getAuthority());
		if (diff != 0) return diff;

		diff = this.accessPermission.getPermission().compareTo(o.accessPermission.getPermission());
		if (diff != 0) return diff;

		return this.accessPermission.getAccessStatus().compareTo(o.accessPermission.getAccessStatus());
	}
}