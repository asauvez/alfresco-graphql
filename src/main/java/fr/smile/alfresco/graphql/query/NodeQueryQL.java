package fr.smile.alfresco.graphql.query;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.repo.nodelocator.SharedHomeNodeLocator;
import org.alfresco.repo.nodelocator.SitesHomeNodeLocator;
import org.alfresco.repo.nodelocator.UserHomeNodeLocator;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.QueryConsistency;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.smile.alfresco.graphql.helper.AbstractQLModel;
import fr.smile.alfresco.graphql.helper.PredicateHelper;
import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.schema.DataFetchingEnvironment;

public class NodeQueryQL extends AbstractQLModel {

	private static Log log = LogFactory.getLog(NodeQueryQL.class);
	
	public NodeQueryQL(QueryContext queryContext) {
		super(queryContext);
	}

	private NodeQL getNodeByLocator(String locatorName) {
		return newNode(getQueryContext().getNodeLocatorService().getNode(locatorName, null, null));
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
	
	public ResultSetQL getQueryNative(DataFetchingEnvironment env) {
		return query(env, sp -> {});
	}
	public ResultSetQL getQuery(DataFetchingEnvironment env) {
		return query(env, sp -> {});
	}
	public Optional<NodeQL> getQueryFirst(DataFetchingEnvironment env) {
		return query(env, sp -> {
			sp.setMaxItems(1);
		}).getNodes().stream().findFirst();
	}
	public Optional<NodeQL> getQueryUnique(DataFetchingEnvironment env) {
		ResultSetQL resultSet = query(env, sp -> {
			sp.setMaxItems(2);
		});
		List<NodeQL> nodes = resultSet.getNodes();
		if (nodes.size() > 1) {
			throw new IllegalStateException("There should not be more than one result but got " + resultSet.getNumberFound());
		}
		return nodes.stream().findFirst();
	}
	
	private ResultSetQL query(DataFetchingEnvironment env, Consumer<SearchParameters> consumer) {
		Object queryObject = env.getArgument("query");
		@SuppressWarnings("unchecked")
		String query = (queryObject instanceof String) 
				? (String) queryObject 
				: PredicateHelper.getQuery(getNamespaceService(), (List<Map<String, Object>>) queryObject);
		
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(query);
		searchParameters.setLanguage(env.getArgumentOrDefault("language", SearchService.LANGUAGE_FTS_ALFRESCO));
		searchParameters.setMaxItems(env.getArgumentOrDefault("maxItems", -1));
		searchParameters.setSkipCount(env.getArgumentOrDefault("skipCount", 0));
		searchParameters.setQueryConsistency(QueryConsistency.valueOf(env.getArgument("queryConsistency")));
		searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

		consumer.accept(searchParameters);
		
		log.debug("Query: " + searchParameters.getQuery());

		List<Map<String, Object>> sorts = env.getArgumentOrDefault("sort", Collections.emptyList());
		for (Map<String, Object> sort : sorts) {
			String property = (String) sort.get("property");
			String direction = (String) sort.get("direction");
			searchParameters.addSort(
					getQName(property).toPrefixString(getNamespaceService()), 
					(direction == null) || "ASCENDING".equals(direction));
		}
		
		ResultSet resultSet = getSearchService().query(searchParameters);
		ResultSetQL resultSetQL = new ResultSetQL(getQueryContext(), resultSet);
		getQueryContext().addCloseable(resultSetQL);
		return resultSetQL;
	}
}