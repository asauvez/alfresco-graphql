package fr.smile.alfresco.graphql.query;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;

import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.schema.DataFetchingEnvironment;

public class NodeQL extends AbstractQLModel implements Comparable<NodeQL> {

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
	
	@Override
	public int compareTo(NodeQL o) {
		return nodeRef.getId().compareTo(o.nodeRef.getId());
	}
	
	public String getType() {
		return getNodeService().getType(nodeRef).toPrefixString(getNamespaceService());
	}
	public boolean getIsContent() {
		return getQueryContext().getDictionaryService().isSubClass(getNodeService().getType(nodeRef), ContentModel.TYPE_CONTENT);
	}
	public boolean getIsFolder() {
		return getQueryContext().getDictionaryService().isSubClass(getNodeService().getType(nodeRef), ContentModel.TYPE_FOLDER);
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

	// ======= URL ==============================================================
	
	public String getOnlineEditionUrl() throws IOException {
		return getQueryContext().getDocumentLinkHelper().getOnlineEditionUrl(nodeRef);
	}
	
	public String getWebDavUrl() {
		return getQueryContext().getDocumentLinkHelper().getWebDavUrl(nodeRef);
	}
	public String getShareUrl() {
		return getQueryContext().getDocumentLinkHelper().getShareUrl(nodeRef);
	}
	
	// ======= Properties ==============================================================
	
	public Optional<String> getPropertyAsString(DataFetchingEnvironment env) {
		QName propertyName = getQName(env.getArgument("name"));
		return getProperty(nodeRef, propertyName)
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
	
	public Optional<String> getName(DataFetchingEnvironment env) throws FileExistsException, FileNotFoundException {
		String setValue = env.getArgument("setValue");
		if (setValue != null) {
			getFileFolderService().rename(nodeRef, setValue);
		}
		return getProperty(nodeRef, ContentModel.PROP_NAME);
	}

	private <T extends Serializable> Optional<T> getProperty(DataFetchingEnvironment env, QName property) {
		T setValue = env.getArgument("setValue");
		if (setValue != null) {
			getNodeService().setProperty(nodeRef, property, setValue);
		}
		
		return getProperty(nodeRef, property);
	}
	public Optional<String> getTitle(DataFetchingEnvironment env) {
		return getProperty(env, ContentModel.PROP_TITLE);
	}

	public Optional<String> getDescription(DataFetchingEnvironment env) {
		return getProperty(env, ContentModel.PROP_DESCRIPTION);
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

	public Optional<ContentDataQL> getContent(DataFetchingEnvironment env) {
		QName property = getQName(env.getArgument("property"));
		String rendition = env.getArgument("rendition");
		if (rendition != null) {
			return Optional.of(getQueryContext().getRenditionService2().getRenditionByName(nodeRef, rendition))
					.flatMap(assoc -> newNode(assoc.getChildRef()).getContent(property));
		}
		return getContent(property);
	}
	public ContentDataQL getContentCreate(DataFetchingEnvironment env) {
		QName property = getQName(env.getArgument("property"));
		return new ContentDataQL(getQueryContext(), nodeRef, property, null);
	}
	public Optional<ContentDataQL> getContent(QName property) {
		Optional<ContentData> contentData = getProperty(nodeRef, property);
		return contentData
				.map(data -> new ContentDataQL(getQueryContext(), nodeRef, property, data));
	}

	// ======= Permissions ==============================================================

	private AccessPermissionQL newAccessPermission(AccessPermission accessPermission) {
		return new AccessPermissionQL(this, accessPermission);
	}
	public boolean getInheritParentPermissions(DataFetchingEnvironment env) {
		Boolean setValue = env.getArgument("setValue");
		if (setValue != null) {
			getPermissionService().setInheritParentPermissions(nodeRef, setValue);
		}
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
	public boolean getHasPermission(DataFetchingEnvironment env) {
		String permission = env.getArgument("permission");
		return getPermissionService().hasPermission(nodeRef, permission) == AccessStatus.ALLOWED;
	}
	public boolean getHasWritePermission() {
		return getPermissionService().hasPermission(nodeRef, PermissionService.WRITE) == AccessStatus.ALLOWED;
	}
	public boolean getHasDeletePermission() {
		return getPermissionService().hasPermission(nodeRef, PermissionService.DELETE) == AccessStatus.ALLOWED;
	}
	public Set<String> getSettablePermissions() {
		return getPermissionService().getSettablePermissions(nodeRef);
	}
	public boolean getSetPermission(DataFetchingEnvironment env) {
		String authority = env.getArgument("authority");
		String permission = env.getArgument("permission");
		boolean allow = env.getArgument("allow");
		getPermissionService().setPermission(nodeRef, authority, permission, allow);
		
		return true; 
	}
	public boolean getDeletePermission(DataFetchingEnvironment env) {
		String authority = env.getArgument("authority");
		String permission = env.getArgument("permission");
		getPermissionService().deletePermission(nodeRef, authority, permission);
		
		return true; 
	}
	
	// ======= Associations ==============================================================

	public Optional<NodeQL> getPrimaryParent() {
		return Optional.ofNullable(getNodeService().getPrimaryParent(nodeRef).getParentRef())
			.map(parent -> newNode(parent));
	}
	public List<NodeQL> getPrimaryParents(DataFetchingEnvironment env) {
		int ignoreFirst = env.getArgument("ignoreFirst");
		boolean reverse = env.getArgument("reverse");

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
	public List<NodeQL> getParents(DataFetchingEnvironment env) {
		QNamePattern assocType = getQNameFilter(env.getArgument("assocType"));
		return getNodeService().getParentAssocs(nodeRef, assocType, null).stream()
			.map(assoc -> newNode(assoc.getChildRef()))
			.collect(Collectors.toList());
	}
	public List<NodeQL> getChildren(DataFetchingEnvironment env) {
		QNamePattern assocType = getQNameFilter(env.getArgument("assocType"));
		return getChildren(assocType, env);
	}
	public List<NodeQL> getChildrenContains(DataFetchingEnvironment env) {
		return getChildren(ContentModel.ASSOC_CONTAINS, env);
	}
	private List<NodeQL> getChildren(QNamePattern assocType, DataFetchingEnvironment env) {
		Comparator<ChildAssociationRef> comparator = new Comparator<ChildAssociationRef>() {
			@Override public int compare(ChildAssociationRef o1, ChildAssociationRef o2) { return 0; }
		};
		List<Map<String, Object>> sorts = env.getArgumentOrDefault("sort", Collections.emptyList());
		for (Map<String, Object> sort : sorts) {
			QName property = getQName((String) sort.get("property"));
			String direction = (String) sort.get("direction");
			
			@SuppressWarnings("rawtypes")
			Comparator<ChildAssociationRef> fieldComparator = Comparator.comparing(
					assoc -> (Comparable) getNodeService().getProperty(assoc.getChildRef(), property),
					Comparator.nullsLast(Comparator.naturalOrder()));
			if (direction != null && "DESCENDING".equals(direction)) {
				fieldComparator = fieldComparator.reversed();
			}
			comparator = comparator.thenComparing(fieldComparator);
		}
		
		return getNodeService().getChildAssocs(nodeRef, assocType, null).stream()
			.sorted(comparator)
			.skip((int) env.getArgument("skipCount"))
			.limit((int) env.getArgument("maxItems"))
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
		return getTargetAssocs(assocType);
	}
	public List<NodeQL> getTargetAssocs(QNamePattern assocType) {
		return getNodeService().getTargetAssocs(nodeRef, assocType).stream()
			.map(assoc -> newNode(assoc.getTargetRef()))
			.collect(Collectors.toList());
	}
	public Optional<NodeQL> getSourceAssoc(DataFetchingEnvironment env) {
		return getSourceAssocs(env).stream().findFirst();
	}
	public Optional<NodeQL> getTargetAssoc(DataFetchingEnvironment env) {
		return getTargetAssocs(env).stream().findFirst();
	}

	public Serializable getPropertyValue(QName property) {
		return getNodeService().getProperty(nodeRef, property);
	}
	public void setPropertyValue(QName property, Serializable setValue) {
		getNodeService().setProperty(nodeRef, property, setValue);
	}
	public void removeProperty(QName property) {
		getNodeService().removeProperty(nodeRef, property);
	}
	
	public NodeQL getAddChild(DataFetchingEnvironment env) {
		QName type = getQName(env.getArgument("type"));
		QName assocType = getQName(env.getArgument("assocType"));
		
		return addChild(env, type, assocType);
	}
	public NodeQL getAddChildFolder(DataFetchingEnvironment env) {
		return addChild(env, ContentModel.TYPE_FOLDER, ContentModel.ASSOC_CONTAINS);
	}
	public NodeQL getAddChildContent(DataFetchingEnvironment env) {
		return addChild(env, ContentModel.TYPE_CONTENT, ContentModel.ASSOC_CONTAINS);
	}

	public NodeQL addChild(DataFetchingEnvironment env, QName type, QName assocType) {
		String name = env.getArgument("name");
		QName assocQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(name));
		Map<QName, Serializable> properties = new HashMap<>();
		properties.put(ContentModel.PROP_NAME, (Serializable) name);
		
		String uuid = env.getArgument("uuid");
		if (uuid != null) {
			properties.put(ContentModel.PROP_NODE_UUID, uuid);
		}
		
		return newNode(getNodeService().createNode(nodeRef, assocType, assocQName, type, properties).getChildRef());
	}

	public boolean getDelete() {
		getNodeService().deleteNode(nodeRef);
		return true;
	}

	// ======= Versions ==============================================================

	public Optional<VersionQL> getCurrentVersion() {
		return Optional.ofNullable(getVersionService().getCurrentVersion(nodeRef))
				.map(v -> new VersionQL(getQueryContext(), v));
	}
	public Optional<List<VersionQL>> getAllVersions() {
		return Optional.ofNullable(getVersionService().getVersionHistory(nodeRef))
				.map(vh -> vh.getAllVersions().stream()
						.map(v -> new VersionQL(getQueryContext(), v))
						.collect(Collectors.toList()));
	}
}