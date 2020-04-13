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

import graphql.schema.DataFetchingEnvironment;

public class NodeQl extends AbstractQlModel {

	private NodeRef nodeRef;

	public NodeQl(ServiceRegistry serviceRegistry, NodeRef nodeRef) {
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
		String name = env.getArgument("name");
		QName propertyName = (name.startsWith(String.valueOf(QName.NAMESPACE_BEGIN))) 
				? QName.createQName(name) 
				: QName.createQName(name, getServiceRegistry().getNamespaceService());
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

	public Optional<DateQl> getCreated() {
		return getProperty(nodeRef, ContentModel.PROP_CREATED)
				.map(o -> new DateQl((Date) o));
	}
	public Optional<String> getCreatedIso() {
		return getCreated().map(DateQl::getIso);
	}

	public Optional<DateQl> getModified() {
		return getProperty(nodeRef, ContentModel.PROP_MODIFIED)
				.map(o -> new DateQl((Date) o));
	}
	public Optional<String> getModifiedIso() {
		return getModified().map(DateQl::getIso);
	}

	public Optional<ContentData> getContent() {
		return getProperty(nodeRef, ContentModel.PROP_CONTENT);
	}

	// ======= Associations ==============================================================

	public Optional<NodeQl> getPrimaryParent() {
		return Optional.ofNullable(getNodeService().getPrimaryParent(nodeRef))
			.map(assoc -> newNode(assoc.getParentRef()));
	}
	public List<NodeQl> getParents() {
		return getNodeService().getParentAssocs(nodeRef).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQl> getChildren() {
		return getNodeService().getChildAssocs(nodeRef).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQl> getChildrenContains() {
		return getNodeService().getChildAssocs(nodeRef, ContentModel.ASSOC_CONTAINS, null).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
}