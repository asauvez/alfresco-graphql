package fr.smile.alfresco.graphql.helper;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.rendition2.RenditionService2;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;

public class QueryContext {

	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private NamespaceService namespaceService;
	private AuthorityService authorityService;
	private PermissionService permissionService;
	private ContentService contentService;
	private SearchService searchService;
	private NodeLocatorService nodeLocatorService;
	private SysAdminParams sysAdminParams;
	private RenditionService2 renditionService2;
	private FileFolderService fileFolderService;
	
	@SuppressWarnings("deprecation")
	public QueryContext(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		
		nodeService = serviceRegistry.getNodeService();
		namespaceService = serviceRegistry.getNamespaceService();
		authorityService = serviceRegistry.getAuthorityService();
		permissionService = serviceRegistry.getPermissionService();
		contentService = serviceRegistry.getContentService();
		searchService = serviceRegistry.getSearchService();
		nodeLocatorService = serviceRegistry.getNodeLocatorService();
		sysAdminParams = serviceRegistry.getSysAdminParams();
		renditionService2 = serviceRegistry.getRenditionService2();
		fileFolderService = serviceRegistry.getFileFolderService();
	}
	
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
	
	public NodeService getNodeService() {
		return nodeService;
	}
	public NamespacePrefixResolver getNamespaceService() {
		return namespaceService;
	}
	public AuthorityService getAuthorityService() {
		return authorityService;
	}
	public PermissionService getPermissionService() {
		return permissionService;
	}
	public ContentService getContentService() {
		return contentService;
	}
	public SearchService getSearchService() {
		return searchService;
	}
	public NodeLocatorService getNodeLocatorService() {
		return nodeLocatorService;
	}
	public SysAdminParams getSysAdminParams() {
		return sysAdminParams;
	}
	public RenditionService2 getRenditionService2() {
		return renditionService2;
	}
	public FileFolderService getFileFolderService() {
		return fileFolderService;
	}
}
