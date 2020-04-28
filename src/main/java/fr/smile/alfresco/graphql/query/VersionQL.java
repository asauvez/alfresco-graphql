package fr.smile.alfresco.graphql.query;

import org.alfresco.service.cmr.version.Version;

import fr.smile.alfresco.graphql.helper.QueryContext;

public class VersionQL extends AbstractQLModel {

	private Version version;

	public VersionQL(QueryContext queryContext, Version version) {
		super(queryContext);
		this.version = version;
	}

	public String getVersionLabel() {
		return version.getVersionLabel();
	}

	public String getVersionType() {
		return version.getVersionType().name();
	}

	public String getDescription() {
		return version.getDescription();
	}

	public DateQL getFrozenModifiedDate() {
		return new DateQL(version.getFrozenModifiedDate());
	}

	public AuthorityQL getFrozenModifier() {
		return newAuthority(version.getFrozenModifier());
	}

	public NodeQL getFrozenStateNode() {
		return newNode(version.getFrozenStateNodeRef());
	}
}