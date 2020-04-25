package fr.smile.alfresco.graphql.query;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;

import fr.smile.alfresco.graphql.helper.QueryContext;

public class AuthorityQL extends AbstractQLModel {

	private String name;

	public AuthorityQL(QueryContext queryContext, String name) {
		super(queryContext);
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
	public Optional<String> getPropertyAsString(QName name) {
		return getNodeRefInternal().flatMap(nodeRef -> getPropertyString(nodeRef, name))
				.map(Object::toString);
	}
	public Optional<NodeQL> getHomeFolder() {
		return getNodeRefInternal().flatMap(nodeRef -> {
			Optional<NodeRef> homeFolder = getProperty(nodeRef, ContentModel.PROP_HOMEFOLDER);
			return homeFolder.map(this::newNode);
		});
	}	
	public Optional<ContentDataQL> getAvatar() {
		return getNode()
			.flatMap(node -> node.getTargetAssocs(ContentModel.ASSOC_AVATAR).stream().findFirst())
			.flatMap(avatar -> avatar.getContent(ContentModel.PROP_CONTENT));
	}
	
	public Optional<NodeQL> getNode() {
		return getNodeRefInternal().map(this::newNode);
	}
	
	public List<AuthorityQL> getContainedAuthorities(AuthorityType type, boolean immediate) {
		return getAuthorityService().getContainedAuthorities(type, name, immediate).stream()
				.map(s -> newAuthority(s))
				.collect(Collectors.toList());
	}
	public List<AuthorityQL> getContainingAuthorities(AuthorityType type, boolean immediate) {
		return getAuthorityService().getContainingAuthorities(type, name, immediate).stream()
				.map(s -> newAuthority(s))
				.collect(Collectors.toList());
	}
}