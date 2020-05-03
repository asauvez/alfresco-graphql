package fr.smile.alfresco.graphql.query;

import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.PermissionDeniedException;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.descriptor.DescriptorService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.ConcurrencyFailureException;

import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.schema.DataFetchingEnvironment;

public class SystemQueryQL extends AbstractQLModel {

	private static Log log = LogFactory.getLog(SystemQueryQL.class);

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
	
	
	private static int compteur = 0;
	public boolean getTestConcurentException(DataFetchingEnvironment env) {
		int modulo = env.getArgument("modulo");
		
		log.info("TestConcurentException " + compteur);
		
		if (compteur ++ < modulo) {
			log.error("==> TestConcurentException");
			throw new ConcurrencyFailureException("TestConcurentException");
		} else {
			compteur = 0;
		}
		return true;
	}
}