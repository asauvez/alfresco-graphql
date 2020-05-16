package fr.smile.alfresco.graphql.query;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import fr.smile.alfresco.graphql.helper.AlfrescoDataType;
import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.schema.DataFetchingEnvironment;

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
	
	@SuppressWarnings("unchecked")
	public Object getPropertyValue(DataFetchingEnvironment env, QName property, AlfrescoDataType alfrescoDataType) {
		NodeQL node = getNode();

		Serializable setValue = env.getArgument("setValue");
		if (setValue != null) {
			node.setPropertyValue(property, setValue);
		}
		boolean remove = env.getArgument("remove");
		if (remove) {
			node.removeProperty(property);
		}
		
		Serializable value = node.getPropertyValue(property);
		
		Serializable append = env.getArgument("append");
		if (append != null) {
			List<Serializable> list = (List<Serializable>) value;
			list.add(append);
			node.setPropertyValue(property, value);
		}

		Integer increment = env.getArgument("increment");
		if (increment != null) {
			Number number = (Number) value;
			value = number.longValue() + increment.intValue();
			node.setPropertyValue(property, value);
		}

		Function<Serializable, Object> function = (item) -> alfrescoDataType.toGraphQl(getQueryContext(), node.getNodeRefInternal(), property, item);
		return (value instanceof List) 
				? ((List<Serializable>) value).stream().map(function).collect(Collectors.toList())
				: Optional.ofNullable(value).map(function);
	}
}