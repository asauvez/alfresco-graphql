package fr.smile.alfresco.graphql.helper;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.rendition2.RenditionService2;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.ActionService;
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
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;

public class QueryContext {

	private ThreadLocal<Map<String, NodeRef>> queryVariablesTL = new ThreadLocal<>();
	private ThreadLocal<Throwable> retryCauseTL = new ThreadLocal<>();
	
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
	private ActionService actionService;
	private WorkflowService workflowService;
	
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
		actionService = serviceRegistry.getActionService();
		workflowService = serviceRegistry.getWorkflowService();
	}
	
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}
	
	public void setQueryVariable(String variable, NodeRef nodeRef) {
		NodeRef oldValue = queryVariablesTL.get().put(variable, nodeRef);
		if (oldValue != null) {
			throw new IllegalStateException("Variable " + variable + " already exists");
		}
	}
	public NodeRef getQueryVariable(String variable) {
		NodeRef nodeRef = queryVariablesTL.get().get(variable);
		if (nodeRef == null) {
			throw new IllegalStateException("Unknown variable " + variable 
					+ ". Known variables = " + queryVariablesTL.get().entrySet());
		}
		return nodeRef;
	}

	public String getQueryVariableAuthority(String variable) {
		NodeRef authorityRef = queryVariablesTL.get().get(variable);
		if (authorityRef != null) {
			QName type = nodeService.getType(authorityRef);
			if (dictionaryService.isSubClass(type, ContentModel.TYPE_AUTHORITY_CONTAINER)) {
				return (String) nodeService.getProperty(authorityRef, ContentModel.PROP_AUTHORITY_NAME);
			} else if (dictionaryService.isSubClass(type, ContentModel.TYPE_PERSON)) {
				return (String) nodeService.getProperty(authorityRef, ContentModel.PROP_USERNAME);
			}
		}

		return variable;
	}
	
	public <T> T executeQuery(RetryingTransactionCallback<T> callback) throws Throwable {
		queryVariablesTL.set(new HashMap<>());
		try {
			T result = callback.execute();
			
			Throwable retryCause = retryCauseTL .get();
			if (retryCause!= null) {
				throw retryCause;
			}
			
			return result;
		} finally {
			queryVariablesTL.remove();
			retryCauseTL.remove();
		}
	}
	
	public void setRetryException(Throwable retryCause) {
		this.retryCauseTL.set(retryCause);
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
	public ActionService getActionService() {
		return actionService;
	}
	public WorkflowService getWorkflowService() {
		return workflowService;
	}
}
