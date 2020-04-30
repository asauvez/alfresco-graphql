package fr.smile.alfresco.graphql.query;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import fr.smile.alfresco.graphql.helper.QueryContext;

public class ContainerNodeQL extends AbstractQLModel {

	private NodeRef nodeRef;
	private QName container;

	public ContainerNodeQL(QueryContext queryContext, NodeRef nodeRef, QName container) {
		super(queryContext);
		this.nodeRef = nodeRef;
		this.container = container;
	}

	public NodeRef getNodeRef() {
		return nodeRef;
	}
	public NodeQL getNode() {
		return newNode(nodeRef);
	}
	
	public boolean getIsExactType() {
		return getNodeService().getType(nodeRef).equals(container);
	}	
	public boolean getIsSubType() {
		return getQueryContext().getDictionaryService().isSubClass(getNodeService().getType(nodeRef), container);
	}	
	public boolean getSetType() {
		getNodeService().setType(nodeRef, container);
		return true;
	}

	public boolean getHasAspect() {
		return getNodeService().hasAspect(nodeRef, container);
	}	
	public boolean getAddAspect() {
		getNodeService().addAspect(nodeRef, container, null);
		return true;
	}
	public boolean getRemoveAspect() {
		getNodeService().removeAspect(nodeRef, container);
		return true;
	}
}