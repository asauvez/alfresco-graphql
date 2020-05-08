package fr.smile.alfresco.graphql.query;

import java.util.Optional;

import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.alfresco.service.namespace.RegexQNamePattern;

import fr.smile.alfresco.graphql.helper.QueryContext;
import fr.smile.alfresco.graphql.servlet.GraphQlConfigurationBuilder;

public abstract class AbstractQLModel {

	private QueryContext queryContext;

	protected AbstractQLModel(QueryContext queryContext) {
		this.queryContext = queryContext;
	}
	
	public QueryContext getQueryContext() {
		return queryContext;
	}
	
	protected NodeService getNodeService() {
		return queryContext.getNodeService();
	}
	protected NamespacePrefixResolver getNamespaceService() {
		return queryContext.getNamespaceService();
	}
	protected AuthorityService getAuthorityService() {
		return queryContext.getAuthorityService();
	}
	protected PermissionService getPermissionService() {
		return queryContext.getPermissionService();
	}
	protected ContentService getContentService() {
		return queryContext.getContentService();
	}
	protected SearchService getSearchService() {
		return queryContext.getSearchService();
	}
	protected NodeLocatorService getNodeLocatorService() {
		return queryContext.getNodeLocatorService();
	}
	protected FileFolderService getFileFolderService() {
		return queryContext.getFileFolderService();
	}
	protected VersionService getVersionService() {
		return queryContext.getVersionService();
	}
	protected CheckOutCheckInService getCheckOutCheckInService() {
		return queryContext.getCheckOutCheckInService();
	}
	
	protected NodeQL newNode(NodeRef nodeRef) {
		return new NodeQL(queryContext, nodeRef);
	}
	protected AuthorityQL newAuthority(String name) {
		return new AuthorityQL(queryContext, name);
	}

	@SuppressWarnings("unchecked")
	protected <T> Optional<T> getProperty(NodeRef nodeRef, QName propertyName) {
		return Optional.ofNullable((T) getNodeService().getProperty(nodeRef, propertyName));
	}
	protected Optional<String> getPropertyString(NodeRef nodeRef, QName propertyName) {
		return Optional.ofNullable((String) getNodeService().getProperty(nodeRef, propertyName));
	}
	protected QName getQName(String name) {
		return GraphQlConfigurationBuilder.getQName(name);
	}
	protected QNamePattern getQNameFilter(String name) {
		if (name == null) {
			return RegexQNamePattern.MATCH_ALL;
		} else {
			return getQName(name);
		}
	}
}