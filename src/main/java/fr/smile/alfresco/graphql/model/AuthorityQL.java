package fr.smile.alfresco.graphql.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;

import graphql.schema.DataFetchingEnvironment;

public class AuthorityQL extends AbstractQLModel {

	private String name;

	public AuthorityQL(ServiceRegistry serviceRegistry, String name) {
		super(serviceRegistry);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	
	private Optional<NodeRef> getNodeRefInternal() {
		return Optional.ofNullable(getAuthorityService().getAuthorityNodeRef(name));
	}
	public Optional<String> getNodeRef() {
		return getNodeRefInternal().map(NodeRef::toString);
	}
	public Optional<String> getUuid() {
		return getNodeRefInternal().map(NodeRef::getId);
	}
	public String getDisplayName() {
		return getAuthorityService().getAuthorityDisplayName(name);
	}
	public String getShortName() {
		return getAuthorityService().getShortName(name);
	}
	public AuthorityType getType() {
		return AuthorityType.getAuthorityType(name);
	}
	
	public Optional<String> getFirstName() {
		return getNodeRefInternal().flatMap(nodeRef -> getPropertyString(nodeRef, ContentModel.PROP_FIRSTNAME));
	}
	public Optional<String> getLastName() {
		return getNodeRefInternal().flatMap(nodeRef -> getPropertyString(nodeRef, ContentModel.PROP_LASTNAME));
	}
	public Optional<String> getEmail() {
		return getNodeRefInternal().flatMap(nodeRef -> getPropertyString(nodeRef, ContentModel.PROP_EMAIL));
	}
	public Optional<String> getPropertyAsString(DataFetchingEnvironment env) {
		QName propertyName = getQName(env.getArgument("name"));
		return getNodeRefInternal().flatMap(nodeRef -> getPropertyString(nodeRef, propertyName))
				.map(Object::toString);
	}
	
	
	public List<AuthorityQL> getContainedAuthorities(DataFetchingEnvironment env) {
		String typeS = env.getArgument("type");
		AuthorityType type = (typeS != null) ? AuthorityType.valueOf(typeS) : null;
		boolean immediate = env.getArgument("immediate");
		return getAuthorityService().getContainedAuthorities(type, name, immediate).stream()
				.map(s -> newAuthority(s))
				.collect(Collectors.toList());
	}
	public List<AuthorityQL> getContainingAuthorities(DataFetchingEnvironment env) {
		String typeS = env.getArgument("type");
		AuthorityType type = (typeS != null) ? AuthorityType.valueOf(typeS) : null;
		boolean immediate = env.getArgument("immediate");
		return getAuthorityService().getContainingAuthorities(type, name, immediate).stream()
				.map(s -> newAuthority(s))
				.collect(Collectors.toList());
	}
}