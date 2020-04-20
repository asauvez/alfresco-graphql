package fr.smile.alfresco.graphql.query;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.UrlUtil;
import org.apache.commons.io.IOUtils;

import fr.smile.alfresco.graphql.helper.AbstractQLModel;
import fr.smile.alfresco.graphql.helper.QueryContext;
import graphql.schema.DataFetchingEnvironment;

public class ContentReaderQL extends AbstractQLModel {

	private NodeRef nodeRef;
	private QName property;
	private ContentReader reader;

	public ContentReaderQL(QueryContext queryContext, NodeRef nodeRef, QName property, ContentReader reader) {
		super(queryContext);
		this.nodeRef = nodeRef;
		this.property = property;
		this.reader = reader;
	}

	public String getProperty() {
		return property.toPrefixString(getNamespaceService());
	}
	public String getMimetype() {
		return reader.getMimetype();
	}
	public int getSize() {
		if (reader.getSize() > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) reader.getSize();
	}
	public String getEncoding() {
		return reader.getEncoding();
	}
	public Optional<String> getLocale() {
		return Optional.ofNullable(reader.getLocale())
				.map(locale -> locale.toString());
	}

	public String getAsString(DataFetchingEnvironment env) {
		return reader.getContentString();
	}
	public String getAsBase64(DataFetchingEnvironment env) throws IOException {
		try (InputStream input = reader.getContentInputStream()) {
			byte[] buf = IOUtils.toByteArray(input);
			return Base64.getEncoder().encodeToString(buf);
		}
	}
	
	public String getDownloadUrl() {
		// TODO manage propertis other than cm:content
		// TODO manage rendition
		
		SysAdminParams sysAdminParams = getQueryContext().getSysAdminParams();
		return UrlUtil.getAlfrescoUrl(sysAdminParams) + "/s/api/node/" 
			+ nodeRef.getStoreRef().getProtocol() + "/" + nodeRef.getStoreRef().getIdentifier() + "/" 
			+ nodeRef.getId() + "/content";
	}
}