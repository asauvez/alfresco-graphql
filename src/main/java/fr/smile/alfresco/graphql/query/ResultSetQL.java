package fr.smile.alfresco.graphql.query;

import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.service.cmr.repository.NodeRef;

import fr.smile.alfresco.graphql.helper.AbstractQLModel;
import fr.smile.alfresco.graphql.helper.QueryContext;

public class ResultSetQL extends AbstractQLModel {

	private List<NodeRef> nodeRefs;
	private long numberFound;

	public ResultSetQL(QueryContext queryContext, List<NodeRef> nodeRefs, long numberFound) {
		super(queryContext);
		this.nodeRefs = nodeRefs;
		this.numberFound = numberFound;
	}

	public List<NodeQL> getNodes() {
		return nodeRefs.stream()
			.map(this::newNode)
			.collect(Collectors.toList());
	}
	
	public int getNumberFound() {
		return (int) numberFound;
	}
}