package fr.smile.alfresco.graphql.model;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.PermissionDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.descriptor.DescriptorService;

public class SystemQueryQL extends AbstractQLModel {

	public SystemQueryQL(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
	}
	
	protected DescriptorService getDescriptor() {
		checkAdmin();
		return getServiceRegistry().getDescriptorService();
	}
	
	public List<ModuleDetails> getModules() {
		checkAdmin();
		return getServiceRegistry().getModuleService().getAllModules();
	}
	public List<ModuleDetails> getMissingModules() {
		checkAdmin();
		return getServiceRegistry().getModuleService().getMissingModules();
	}
	
	private void checkAdmin() {
		if (! getServiceRegistry().getAuthorityService().isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new PermissionDeniedException("Not an admin");
		}
	}
}