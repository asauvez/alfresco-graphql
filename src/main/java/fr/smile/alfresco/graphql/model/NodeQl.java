package fr.smile.alfresco.graphql.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

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

	public String getName() {
		return getProperty(nodeRef, ContentModel.PROP_NAME);
	}

	public String getTitle() {
		return getProperty(nodeRef, ContentModel.PROP_TITLE);
	}

	public String getDescription() {
		return getProperty(nodeRef, ContentModel.PROP_DESCRIPTION);
	}

	public Optional<DateQl> getCreated() {
		return DateQl.of(getProperty(nodeRef, ContentModel.PROP_CREATED));
	}

	public Optional<DateQl> getModified() {
		return DateQl.of(getProperty(nodeRef, ContentModel.PROP_MODIFIED));
	}
	
	public Optional<NodeQl> getPrimaryParent() {
		return Optional.ofNullable(getNodeService().getPrimaryParent(nodeRef))
			.map(assoc -> newNode(assoc.getParentRef()));
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