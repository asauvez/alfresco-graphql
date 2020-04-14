package fr.smile.alfresco.graphql.model;

import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.search.ResultSet;

public class ResultSetQL extends AbstractQLModel implements AutoCloseable {

	private ResultSet resultSet;

	public ResultSetQL(ServiceRegistry serviceRegistry, ResultSet resultSet) {
		super(serviceRegistry);
		this.resultSet = resultSet;
	}
	
	public List<NodeQL> getNodes() {
		return resultSet.getNodeRefs().stream()
			.map(this::newNode)
			.collect(Collectors.toList());
	}
	
	public int getNumberFound() {
		return (int) resultSet.getNumberFound();
	}
	
	@Override
	public void close() {
		resultSet.close();
	}
}