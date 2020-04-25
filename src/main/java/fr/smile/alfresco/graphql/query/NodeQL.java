package fr.smile.alfresco.graphql.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;

import fr.smile.alfresco.graphql.helper.QueryContext;

public class NodeQL extends AbstractQLModel {

	private NodeRef nodeRef;

	public NodeQL(QueryContext queryContext, NodeRef nodeRef) {
		super(queryContext);
		this.nodeRef = nodeRef;
	}

	public String getNodeRef() {
		return nodeRef.toString();
	}
	public NodeRef getNodeRefInternal() {
		return nodeRef;
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
				.sorted()
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
	
	public Optional<String> getPropertyAsString(QName name) {
		return getProperty(nodeRef, name)
				.map(Object::toString);
	}
	public NodeQL getProperties() {
		return this; // managed in global configuration
	}
	
	public static class PropertyValue {
		private String property;
		private Optional<String> valueAsString;
		
		public PropertyValue(String property, Optional<String> valueAsString) {
			this.property = property;
			this.valueAsString = valueAsString;
		}
		public String getProperty() {
			return property;
		}
		public Optional<String> getValueAsString() {
			return valueAsString;
		}
	}
	public List<PropertyValue> getPropertiesList() {
		return getNodeService().getProperties(nodeRef).entrySet().stream()
				.map(entry -> new PropertyValue(
						entry.getKey().toPrefixString(getNamespaceService()), 
						Optional.ofNullable(entry.getValue()).map(Serializable::toString)))
				.collect(Collectors.toList());
	}
	
	public Optional<String> getName(String setValue) throws FileExistsException, FileNotFoundException {
		if (setValue != null) {
			getFileFolderService().rename(nodeRef, setValue);
		}
		return getProperty(nodeRef, ContentModel.PROP_NAME);
	}

	private <T extends Serializable> Optional<T> getProperty(Serializable setValue, QName property) {
		if (setValue != null) {
			getNodeService().setProperty(nodeRef, property, setValue);
		}
		
		return getProperty(nodeRef, property);
	}
	public Optional<String> getTitle(Serializable setValue) {
		return getProperty(setValue, ContentModel.PROP_TITLE);
	}

	public Optional<String> getDescription(Serializable setValue) {
		return getProperty(setValue, ContentModel.PROP_DESCRIPTION);
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
	public Optional<AuthorityQL> getOwner() {
		return getProperty(nodeRef, ContentModel.PROP_OWNER)
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

	public Optional<ContentDataQL> getContent(QName property, String rendition) {
		if (rendition != null) {
			return Optional.of(getQueryContext().getRenditionService2().getRenditionByName(nodeRef, rendition))
					.flatMap(assoc -> newNode(assoc.getChildRef()).getContent(property));
		}
		return getContent(property);
	}
	public ContentDataQL getContentCreate(QName property) {
		return new ContentDataQL(getQueryContext(), nodeRef, property, null);
	}
	public Optional<ContentDataQL> getContent(QName property) {
		Optional<ContentData> contentData = getProperty(nodeRef, property);
		return contentData
				.map(data -> new ContentDataQL(getQueryContext(), nodeRef, property, data));
	}

	// ======= Permissions ==============================================================

	public boolean getInheritParentPermissions() {
		return getPermissionService().getInheritParentPermissions(nodeRef);
	}
	public List<AccessPermissionQL> getPermissions() {
		return getPermissionService().getPermissions(nodeRef).stream()
				.map(this::newAccessPermission)
				.sorted()
				.collect(Collectors.toList());
	}
	public List<AccessPermissionQL> getAllSetPermissions() {
		return getPermissionService().getAllSetPermissions(nodeRef).stream()
				.map(this::newAccessPermission)
				.sorted() // to have predictable tests
				.collect(Collectors.toList());
	}
	public boolean getHasPermission(String permission) {
		return getPermissionService().hasPermission(nodeRef, permission) == AccessStatus.ALLOWED;
	}
	public boolean getHasWritePermission() {
		return getPermissionService().hasPermission(nodeRef, PermissionService.WRITE) == AccessStatus.ALLOWED;
	}
	public boolean getHasDeletePermission() {
		return getPermissionService().hasPermission(nodeRef, PermissionService.DELETE) == AccessStatus.ALLOWED;
	}
	
	
	// ======= Associations ==============================================================

	public Optional<NodeQL> getPrimaryParent() {
		return Optional.ofNullable(getNodeService().getPrimaryParent(nodeRef).getParentRef())
			.map(parent -> newNode(parent));
	}
	public List<NodeQL> getPrimaryParents(int ignoreFirst, boolean reverse) {
		NodeRef parent = getNodeService().getPrimaryParent(nodeRef).getParentRef();
		List<NodeQL> result = new ArrayList<>();
		
		while (parent != null) {
			result.add(newNode(parent));
			parent = getNodeService().getPrimaryParent(parent).getParentRef();
		}
		result = result.subList(0, result.size() - ignoreFirst);
		
		if (reverse) {
			Collections.reverse(result);
		}
		return result;
	}
	public List<NodeQL> getParents(String assocType) {
		QNamePattern pattern = getQNameFilter(assocType);
		return getNodeService().getParentAssocs(nodeRef, pattern, null).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQL> getChildren(String assocType, int skipCount, int maxItems) {
		QNamePattern pattern = getQNameFilter(assocType);
		return getChildren(pattern, skipCount, maxItems);
	}
	public List<NodeQL> getChildrenContains(int skipCount, int maxItems) {
		return getChildren(ContentModel.ASSOC_CONTAINS, skipCount, maxItems);
	}
	private List<NodeQL> getChildren(QNamePattern assocType, int skipCount, int maxItems) {
		return getNodeService().getChildAssocs(nodeRef, assocType, null).stream()
			.skip(skipCount)
			.limit(maxItems)
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	} 
	public Optional<NodeQL> getChildByName(String name, QName assocType) {
		return Optional.ofNullable(getNodeService().getChildByName(nodeRef, assocType, name))
			.map(child -> newNode(child));
	}
	public List<NodeQL> getSourceAssocs(QName assocType) {
		return getNodeService().getSourceAssocs(nodeRef, assocType).stream()
			.map(assoc -> newNode(assoc.getSourceRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQL> getTargetAssocs(String assocType) {
		QNamePattern pattern = getQNameFilter(assocType);
		return getTargetAssocs(pattern);
	}
	public List<NodeQL> getTargetAssocs(QNamePattern assocType) {
		return getNodeService().getTargetAssocs(nodeRef, assocType).stream()
			.map(assoc -> newNode(assoc.getTargetRef()))
			.collect(Collectors.toList());
	}
	public Optional<NodeQL> getSourceAssoc(QName assocType) {
		return getSourceAssocs(assocType).stream().findFirst();
	}
	public Optional<NodeQL> getTargetAssoc(String assocType) {
		return getTargetAssocs(assocType).stream().findFirst();
	}

	public Serializable getPropertyValue(QName property) {
		return getNodeService().getProperty(nodeRef, property);
	}
	public void setPropertyValue(QName property, Serializable setValue) {
		getNodeService().setProperty(nodeRef, property, setValue);
	}
	
	public NodeQL getAddChild(String name, QName type, QName assocType) {
		return newNode(getFileFolderService().create(nodeRef, name, type, assocType).getNodeRef());
	}
	public NodeQL getAddChildFolder(String name) {
		return newNode(getFileFolderService().create(nodeRef, name, ContentModel.TYPE_FOLDER).getNodeRef());
	}
	public NodeQL getAddChildContent(String name) {
		return newNode(getFileFolderService().create(nodeRef, name, ContentModel.TYPE_CONTENT).getNodeRef());
	}

	public boolean getDelete() {
		getNodeService().deleteNode(nodeRef);
		return true;
	}
}