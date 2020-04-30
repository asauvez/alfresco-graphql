package fr.smile.alfresco.graphql.query;

import org.alfresco.service.namespace.QName;

public class ContainerNodeQL extends AbstractQLModel {

	private NodeQL node;
	private QName container;

	public ContainerNodeQL(NodeQL node, QName container) {
		super(node.getQueryContext());
		this.node = node;
		this.container = container;
	}

	public NodeQL getNode() {
		return node;
	}
	
	public boolean getIsExactType() {
		return getNodeService().getType(node.getNodeRefInternal()).equals(container);
	}	
	public boolean getIsSubType() {
		return getQueryContext().getDictionaryService().isSubClass(getNodeService().getType(node.getNodeRefInternal()), container);
	}	
	public boolean getSetType() {
		getNodeService().setType(node.getNodeRefInternal(), container);
		return true;
	}

	public boolean getHasAspect() {
		return getNodeService().hasAspect(node.getNodeRefInternal(), container);
	}	
	public boolean getAddAspect() {
		getNodeService().addAspect(node.getNodeRefInternal(), container, null);
		return true;
	}
	public boolean getRemoveAspect() {
		getNodeService().removeAspect(node.getNodeRefInternal(), container);
		return true;
	}
}