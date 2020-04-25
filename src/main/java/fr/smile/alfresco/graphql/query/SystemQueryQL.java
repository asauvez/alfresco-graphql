package fr.smile.alfresco.graphql.query;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.PermissionDeniedException;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.descriptor.DescriptorService;

import fr.smile.alfresco.graphql.helper.QueryContext;

public class SystemQueryQL extends AbstractQLModel {

	public SystemQueryQL(QueryContext queryContext) {
		super(queryContext);
	}
	
	protected DescriptorService getDescriptor() {
		checkAdmin();
		return getQueryContext().getServiceRegistry().getDescriptorService();
	}
	
	public List<ModuleDetails> getModules() {
		checkAdmin();
		return getQueryContext().getServiceRegistry().getModuleService().getAllModules();
	}
	public List<ModuleDetails> getMissingModules() {
		checkAdmin();
		return getQueryContext().getServiceRegistry().getModuleService().getMissingModules();
	}
	
	private void checkAdmin() {
		if (! getAuthorityService().isAdminAuthority(AuthenticationUtil.getFullyAuthenticatedUser())) {
			throw new PermissionDeniedException("Not an admin");
		}
	}
}