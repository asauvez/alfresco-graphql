package fr.smile.alfresco.graphql.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.repo.nodelocator.SharedHomeNodeLocator;
import org.alfresco.repo.nodelocator.SitesHomeNodeLocator;
import org.alfresco.repo.nodelocator.UserHomeNodeLocator;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;

import graphql.schema.DataFetchingEnvironment;

public class NodeQueryQl extends AbstractQlModel {

	public NodeQueryQl(ServiceRegistry serviceRegistry) {
		super(serviceRegistry);
	}

	private NodeQl getNodeByLocator(String locatorName) {
		return newNode(getServiceRegistry().getNodeLocatorService().getNode(locatorName, null, null));
	}
	public NodeQl getCompanyHome() {
		return getNodeByLocator(CompanyHomeNodeLocator.NAME);
	}
	public NodeQl getUserHome() {
		return getNodeByLocator(UserHomeNodeLocator.NAME);
	}
	public NodeQl getSharedHome() {
		return getNodeByLocator(SharedHomeNodeLocator.NAME);
	}
	public NodeQl getSitesHome() {
		return getNodeByLocator(SitesHomeNodeLocator.NAME);
	}

	public Optional<NodeQl> getByNodeRef(DataFetchingEnvironment env) {
		NodeRef nodeRef = new NodeRef(env.getArgument("nodeRef"));
		return Optional.ofNullable(getNodeService().exists(nodeRef) ? newNode(nodeRef) : null);
	}
	public Optional<NodeQl> getByUuid(DataFetchingEnvironment env) {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, env.getArgument("uuid"));
		return Optional.ofNullable(getNodeService().exists(nodeRef) ? newNode(nodeRef) : null);
	}
	
	public List<NodeQl> query(DataFetchingEnvironment env) {
		SearchParameters searchParameters = env.getArgument("params");
		ResultSet resultSet = getServiceRegistry().getSearchService().query(searchParameters);
		try {
			return resultSet.getNodeRefs().stream()
					.map(this::newNode)
					.collect(Collectors.toList());
		} finally {
			resultSet.close();
		}
	}
}