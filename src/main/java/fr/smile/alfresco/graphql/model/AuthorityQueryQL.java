package fr.smile.alfresco.graphql.model;

import java.util.Optional;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;

import graphql.schema.DataFetchingEnvironment;

public class AuthorityQueryQL extends AbstractQLModel {

	public AuthorityQueryQL(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
	}

	public AuthorityQL getCurrentUser() {
		return newAuthority(AuthenticationUtil.getFullyAuthenticatedUser());
	}
	
	public Optional<AuthorityQL> getByName(DataFetchingEnvironment env) {
		String name = env.getArgument("name");
		return Optional.ofNullable(getAuthorityService().authorityExists(name) ? newAuthority(name) : null);
	}
}