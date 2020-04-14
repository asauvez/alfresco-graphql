package fr.smile.alfresco.graphql.model;

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

import graphql.schema.DataFetchingEnvironment;

public class NodeQueryQL extends AbstractQLModel {

	public NodeQueryQL(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
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
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setQuery(env.getArgument("query"));
		searchParameters.setLanguage(env.getArgument("language"));
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