package fr.smile.alfresco.graphql.query;

import java.util.Optional;

import org.alfresco.repo.security.authentication.AuthenticationUtil;

import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.schema.DataFetchingEnvironment;

public class AuthorityQueryQL extends AbstractQLModel {

	public AuthorityQueryQL(QueryContext queryContext) {
		super(queryContext);
	}

	public AuthorityQL getCurrentUser() {
		return newAuthority(AuthenticationUtil.getFullyAuthenticatedUser());
	}
	
	public Optional<AuthorityQL> getByName(DataFetchingEnvironment env) {
		String name = env.getArgument("name");
		return Optional.ofNullable(getAuthorityService().authorityExists(name) ? newAuthority(name) : null);
	}
}