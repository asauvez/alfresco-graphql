package fr.smile.alfresco.graphql.model;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
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
	
	public QName getType() {
		return getNodeService().getType(nodeRef);
	}
	public Set<QName> getAspects() {
		return getNodeService().getAspects(nodeRef);
	}
	public String getPathDisplay() {
		return getNodeService().getPath(nodeRef).toDisplayPath(getNodeService(), getServiceRegistry().getPermissionService());
	}
	public String getPathPrefixString() {
		return getNodeService().getPath(nodeRef).toPrefixString(getServiceRegistry().getNamespaceService());
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

	public Optional<DateQL> getModified() {
		return getProperty(nodeRef, ContentModel.PROP_MODIFIED)
				.map(o -> new DateQL((Date) o));
	}
	public Optional<String> getModifiedIso() {
		return getModified().map(DateQL::getIso);
	}

	public Optional<ContentData> getContent() {
		return getProperty(nodeRef, ContentModel.PROP_CONTENT);
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