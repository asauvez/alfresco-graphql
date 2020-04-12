package fr.smile.alfresco.graphql.model;

import java.util.Date;
import java.util.Optional;

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
		return (String) getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
	}

	public String getTitle() {
		return (String) getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_TITLE);
	}

	public String getDescription() {
		return (String) getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_DESCRIPTION);
	}

	public Optional<DateQl> getCreated() {
		return DateQl.of((Date) getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_CREATED));
	}

	public Optional<DateQl> getModified() {
		return DateQl.of((Date) getServiceRegistry().getNodeService().getProperty(nodeRef, ContentModel.PROP_MODIFIED));
	}
}