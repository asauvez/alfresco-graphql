package fr.smile.alfresco.graphql.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.UrlUtil;
import org.apache.commons.io.IOUtils;

public class ContentReaderQL extends AbstractQLModel {

	private NodeRef nodeRef;
	private ContentReader reader;

	public ContentReaderQL(ServiceRegistry serviceRegistry, NodeRef nodeRef, ContentReader reader) {
		super(serviceRegistry);
		this.nodeRef = nodeRef;
		this.reader = reader;
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

	public String getAsString() {
		return reader.getContentString();
	}
	public String getAsBase64() throws IOException {
		try (InputStream input = reader.getContentInputStream()) {
			byte[] buf = IOUtils.toByteArray(input);
			return Base64.getEncoder().encodeToString(buf);
		}
	}
	
	public String getDownloadUrl() {
		// TODO gérer propriété autre que cm:content
		// TODO gérer rendition
		
		SysAdminParams sysAdminParams = getServiceRegistry().getSysAdminParams();
		return UrlUtil.getAlfrescoUrl(sysAdminParams) + "/s/api/node/" 
			+ nodeRef.getStoreRef().getProtocol() + "/" + nodeRef.getStoreRef().getIdentifier() + "/" 
			+ nodeRef.getId() + "/content";
	}
}