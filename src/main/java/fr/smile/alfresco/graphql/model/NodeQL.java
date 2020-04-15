package fr.smile.alfresco.graphql.model;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;

import graphql.schema.DataFetchingEnvironment;

public class NodeQL extends AbstractQLModel {

	private NodeRef nodeRef;

	public NodeQL(ServiceRegistry serviceRegistry, NodeRef nodeRef) {
		super(serviceRegistry);
		this.nodeRef = nodeRef;
	}

	public String getNodeRef() {
		return nodeRef.toString();
	}

	public String getUuid() {
		return nodeRef.getId();
	}
	
	public String getType() {
		return getNodeService().getType(nodeRef).toPrefixString(getNamespaceService());
	}

	public List<String> getAspects() {
		return getNodeService().getAspects(nodeRef).stream()
				.map(qname -> qname.toPrefixString(getNamespaceService()))
				.collect(Collectors.toList());
	}
	public String getPathDisplay() {
		return getNodeService().getPath(nodeRef).toDisplayPath(getNodeService(), getPermissionService());
	}
	public String getPathPrefixString() {
		return getNodeService().getPath(nodeRef).toPrefixString(getNamespaceService());
	}
	public String getPathQualifiedName() {
		return getNodeService().getPath(nodeRef).toString();
	}

	// ======= Properties ==============================================================
	
	public Optional<String> getPropertyAsString(DataFetchingEnvironment env) {
		QName propertyName = getQName(env.getArgument("name"));
		return Optional.ofNullable(getProperty(nodeRef, propertyName))
				.map(Object::toString);
	}
	
	public String getName() {
		return getProperty(nodeRef, ContentModel.PROP_NAME).get().toString();
	}

	public String getTitle() {
		return getProperty(nodeRef, ContentModel.PROP_TITLE).get().toString();
	}

	public String getDescription() {
		return getProperty(nodeRef, ContentModel.PROP_DESCRIPTION).get().toString();
	}

	public Optional<DateQL> getCreated() {
		return getProperty(nodeRef, ContentModel.PROP_CREATED)
				.map(o -> new DateQL((Date) o));
	}
	public Optional<String> getCreatedIso() {
		return getCreated().map(DateQL::getIso);
	}
	public Optional<AuthorityQL> getCreator() {
		return getProperty(nodeRef, ContentModel.PROP_CREATOR)
				.map(o -> newAuthority((String) o));
	}
	

	public Optional<DateQL> getModified() {
		return getProperty(nodeRef, ContentModel.PROP_MODIFIED)
				.map(o -> new DateQL((Date) o));
	}
	public Optional<String> getModifiedIso() {
		return getModified().map(DateQL::getIso);
	}
	public Optional<AuthorityQL> getModifier() {
		return getProperty(nodeRef, ContentModel.PROP_MODIFIER)
				.map(o -> newAuthority((String) o));
	}

	public Optional<ContentReaderQL> getContent(DataFetchingEnvironment env) {
		QName property = getQName(env.getArgument("property"));
		Optional<ContentReader> contentData = Optional.ofNullable(getContentService().getReader(nodeRef, property));
		return contentData
				.map(reader -> new ContentReaderQL(getServiceRegistry(), nodeRef, reader));
	}

	// ======= Permissions ==============================================================

	public boolean getInheritParentPermissions() {
		return getPermissionService().getInheritParentPermissions(nodeRef);
	}
	public List<AccessPermissionQL> getPermissions() {
		return getPermissionService().getPermissions(nodeRef).stream()
				.map(this::newAccessPermission)
				.collect(Collectors.toList());
	}
	public List<AccessPermissionQL> getAllSetPermissions() {
		return getPermissionService().getAllSetPermissions(nodeRef).stream()
				.map(this::newAccessPermission)
				.collect(Collectors.toList());
	}
	public boolean getHasPermission(DataFetchingEnvironment env) {
		String permission = env.getArgument("permission");
		return getPermissionService().hasPermission(nodeRef, permission) == AccessStatus.ALLOWED;
	}
	public boolean getHasReadPermission() {
		return getPermissionService().hasReadPermission(nodeRef) == AccessStatus.ALLOWED;
	}
	public boolean getHasWritePermission() {
		return getPermissionService().hasPermission(nodeRef, PermissionService.WRITE) == AccessStatus.ALLOWED;
	}
	public boolean getHasDeletePermission() {
		return getPermissionService().hasPermission(nodeRef, PermissionService.DELETE) == AccessStatus.ALLOWED;
	}
	
	
	// ======= Associations ==============================================================

	public Optional<NodeQL> getPrimaryParent() {
		return Optional.ofNullable(getNodeService().getPrimaryParent(nodeRef))
			.map(assoc -> newNode(assoc.getParentRef()));
	}
	public List<NodeQL> getParents(DataFetchingEnvironment env) {
		QNamePattern assocType = getQNameFilter(env.getArgument("assocType"));
		return getNodeService().getParentAssocs(nodeRef, assocType, null).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQL> getChildren(DataFetchingEnvironment env) {
		QNamePattern assocType = getQNameFilter(env.getArgument("assocType"));
		return getNodeService().getChildAssocs(nodeRef, assocType, null).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQL> getChildrenContains() {
		return getNodeService().getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS, null).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
	public Optional<NodeQL> getChildByName(DataFetchingEnvironment env) {
		String name = env.getArgument("name");
		QName assocType = getQName(env.getArgument("assocType"));
		return Optional.ofNullable(getNodeService().getChildByName(nodeRef, assocType, name))
			.map(child -> newNode(child));
	}
	public List<NodeQL> getSourceAssocs(DataFetchingEnvironment env) {
		QNamePattern assocType = getQNameFilter(env.getArgument("assocType"));
		return getNodeService().getSourceAssocs(nodeRef, assocType).stream()
			.map(assoc -> newNode(assoc.getSourceRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQL> getTargetAssocs(DataFetchingEnvironment env) {
		QNamePattern assocType = getQNameFilter(env.getArgument("assocType"));
		return getNodeService().getTargetAssocs(nodeRef, assocType).stream()
			.map(assoc -> newNode(assoc.getTargetRef()))
			.collect(Collectors.toList());
	}
}