package fr.smile.alfresco.graphql.helper;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.rendition2.RenditionService2;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.DocumentLinkService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;

public class QueryContext {

	private ThreadLocal<Map<String, NodeRef>> queryVariables = new ThreadLocal<>();
	
	private ServiceRegistry serviceRegistry;
	private DocumentLinkHelper documentLinkHelper;
	private NodeService nodeService;
	private NamespaceService namespaceService;
	private AuthorityService authorityService;
	private PermissionService permissionService;
	private ContentService contentService;
	private SearchService searchService;
	private NodeLocatorService nodeLocatorService;
	private RenditionService2 renditionService2;
	private FileFolderService fileFolderService;
	private VersionService versionService;
	private DictionaryService dictionaryService;
	private LockService lockService;
	private DocumentLinkService documentLinkService;

	private CheckOutCheckInService checkOutCheckInService;
	
	public QueryContext(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		
		documentLinkHelper = new DocumentLinkHelper(serviceRegistry);
		nodeService = serviceRegistry.getNodeService();
		namespaceService = serviceRegistry.getNamespaceService();
		authorityService = serviceRegistry.getAuthorityService();
		permissionService = serviceRegistry.getPermissionService();
		contentService = serviceRegistry.getContentService();
		searchService = serviceRegistry.getSearchService();
		nodeLocatorService = serviceRegistry.getNodeLocatorService();
		renditionService2 = serviceRegistry.getRenditionService2();
		fileFolderService = serviceRegistry.getFileFolderService();
		versionService = serviceRegistry.getVersionService();
		dictionaryService = serviceRegistry.getDictionaryService();
		lockService = serviceRegistry.getLockService();
		documentLinkService = serviceRegistry.getDocumentLinkService();
		checkOutCheckInService = serviceRegistry.getCheckOutCheckInService();
	}
	
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
	
	public void setQueryVariable(String variable, NodeRef nodeRef) {
		NodeRef oldValue = queryVariables.get().put(variable, nodeRef);
		if (oldValue != null) {
			throw new IllegalStateException("Variable " + variable + " already exists");
		}
	}
	public NodeRef getQueryVariable(String variable) {
		NodeRef nodeRef = queryVariables.get().get(variable);
		if (nodeRef == null) {
			throw new IllegalStateException("Unknown variable " + variable);
		}
		return nodeRef;
	}
	public <T> T executeQuery(RetryingTransactionCallback<T> callback) throws Throwable {
		queryVariables.set(new HashMap<>());
		try {
			return callback.execute();
		} finally {
			queryVariables.remove();
		}
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
	public RenditionService2 getRenditionService2() {
		return renditionService2;
	}
	public FileFolderService getFileFolderService() {
		return fileFolderService;
	}
	public VersionService getVersionService() {
		return versionService;
	}
	public DocumentLinkHelper getDocumentLinkHelper() {
		return documentLinkHelper;
	}
	public DictionaryService getDictionaryService() {
		return dictionaryService;
	}
	public LockService getLockService() {
		return lockService;
	}
	public DocumentLinkService getDocumentLinkService() {
		return documentLinkService;
	}
	public CheckOutCheckInService getCheckOutCheckInService() {
		return checkOutCheckInService;
	}
}
