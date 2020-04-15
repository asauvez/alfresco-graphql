package fr.smile.alfresco.graphql.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.repo.nodelocator.SharedHomeNodeLocator;
import org.alfresco.repo.nodelocator.SitesHomeNodeLocator;
import org.alfresco.repo.nodelocator.UserHomeNodeLocator;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.smile.alfresco.graphql.helper.PredicateHelper;
import graphql.schema.DataFetchingEnvironment;

public class NodeQueryQL extends AbstractQLModel {

	private static Log log = LogFactory.getLog(NodeQueryQL.class);
	
	private PredicateHelper predicateHelper;
	
	public NodeQueryQL(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
		this.predicateHelper = new PredicateHelper(getNamespaceService());
	}

	private NodeQL getNodeByLocator(String locatorName) {
		return newNode(getServiceRegistry().getNodeLocatorService().getNode(locatorName, null, null));
	}
	public NodeQL getCompanyHome() {
		return getNodeByLocator(CompanyHomeNodeLocator.NAME);
	}
	public NodeQL getUserHome() {
		return getNodeByLocator(UserHomeNodeLocator.NAME);
	}
	public NodeQL getSharedHome() {
		return getNodeByLocator(SharedHomeNodeLocator.NAME);
	}
	public NodeQL getSitesHome() {
		return getNodeByLocator(SitesHomeNodeLocator.NAME);
	}

	public Optional<NodeQL> getByNodeRef(DataFetchingEnvironment env) {
		NodeRef nodeRef = new NodeRef(env.getArgument("nodeRef"));
		return Optional.ofNullable(getNodeService().exists(nodeRef) ? newNode(nodeRef) : null);
	}
	public Optional<NodeQL> getByUuid(DataFetchingEnvironment env) {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, env.getArgument("uuid"));
		return Optional.ofNullable(getNodeService().exists(nodeRef) ? newNode(nodeRef) : null);
	}
	
	public ResultSetQL getQuery(DataFetchingEnvironment env) {
		return query(env, 
				env.getArgument("query"), 
				env.getArgument("language"));
	}
	public ResultSetQL getQueryPredicate(DataFetchingEnvironment env) {
		List<Map<String, Object>> predicates = env.getArgument("query");
		String query = predicateHelper.getQuery(predicates);
		return query(env, 
				query, 
				SearchService.LANGUAGE_FTS_ALFRESCO);
	}
	
	private ResultSetQL query(DataFetchingEnvironment env, String query, String language) {
		log.debug("Query: " + query);

		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(language);
		searchParameters.setMaxItems(env.getArgument("maxItems"));
		searchParameters.setSkipCount(env.getArgument("skipCount"));
		searchParameters.setQueryConsistency(QueryConsistency.valueOf(env.getArgument("queryConsistency")));
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		
		// TODO sortdefinition
		
		// TODO close resultset
		
		ResultSet resultSet = getServiceRegistry().getSearchService().query(searchParameters);
		return new ResultSetQL(getServiceRegistry(), resultSet);
	}
}