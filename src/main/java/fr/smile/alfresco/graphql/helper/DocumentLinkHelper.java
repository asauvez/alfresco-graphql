package fr.smile.alfresco.graphql.helper;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.cmr.repository.Path.Element;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.webdav.WebDavService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.UrlUtil;
import org.apache.commons.io.FilenameUtils;

public class DocumentLinkHelper {
	
	private Map<String, String> msProtocoleNames = new HashMap<String, String>() {{
		put("doc", "ms-word");
		put("docx", "ms-word");
		put("docm", "ms-word");
		put("dot", "ms-word");
		put("dotx", "ms-word");
		put("dotm", "ms-word");
		put("xls", "ms-excel");
		put("xlsx", "ms-excel");
		put("xlsb", "ms-excel");
		put("xlsm", "ms-excel");
		put("xlt", "ms-excel");
		put("xltx", "ms-excel");
		put("xltm", "ms-excel");
		put("xlsm", "ms-excel");
		put("ppt", "ms-powerpoint");
		put("pptx", "ms-powerpoint");
		put("pot", "ms-powerpoint");
		put("potx", "ms-powerpoint");
		put("potm", "ms-powerpoint");
		put("pptm", "ms-powerpoint");
		put("potm", "ms-powerpoint");
		put("pps", "ms-powerpoint");
		put("ppsx", "ms-powerpoint");
		put("ppam", "ms-powerpoint");
		put("ppsm", "ms-powerpoint");
		put("sldx", "ms-powerpoint");
		put("sldm", "ms-powerpoint");
	}};

	private WebDavService webDavService;
	private NodeService nodeService;
	private SysAdminParams sysAdminParams;
	private SiteService siteService;
	private FileFolderService fileFolderService;
	
	@SuppressWarnings("deprecation")
	public DocumentLinkHelper(ServiceRegistry serviceRegistry) {
		this.nodeService = serviceRegistry.getNodeService();
		this.sysAdminParams = serviceRegistry.getSysAdminParams();
		this.webDavService = serviceRegistry.getWebDavService();
		this.siteService = serviceRegistry.getSiteService();
		this.fileFolderService = serviceRegistry.getFileFolderService();
	}
	
	/** Inspired by: https://github.com/Alfresco/share/blob/6.0/share/src/main/webapp/components/documentlibrary/actions.js */
	public String getOnlineEditionUrl(NodeRef nodeRef) throws IOException {
		String originalNameDocument = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
		String msType = msProtocoleNames.get(FilenameUtils.getExtension(originalNameDocument).toLowerCase());
		
		String finalurl;
		if (msType == null) {
			// No editor for this type. Return download link
			//finalurl = UrlUtil.getAlfrescoUrl(sysAdminParams) + "/api/-default-/public/alfresco/versions/1/nodes/" + uuid + "/content";
			finalurl = UrlUtil.getAlfrescoUrl(sysAdminParams) + "/s/api/node/" 
					+ nodeRef.getStoreRef().getProtocol() + "/" + nodeRef.getStoreRef().getIdentifier() + "/" 
					+ nodeRef.getId() + "/content";
		} else {
			StringBuilder buf = new StringBuilder()
					.append(msType)
					.append(":ofe|u|")
					.append(UrlUtil.getAlfrescoUrl(sysAdminParams))
					.append("/aos");
			
			Iterator<Element> it = nodeService.getPath(nodeRef).iterator();
			it.next(); // ignore "/"
			it.next(); // ignore company_home
			while (it.hasNext()) {
				QName qName = ((ChildAssocElement) it.next()).getRef().getQName();
				buf.append("/").append(URLEncoder.encode(qName.getLocalName(), "UTF-8").replace("+", "%20"));
			}
			finalurl = buf.toString();
		}
		return finalurl;
	}
	
	public String getWebDavUrl(NodeRef nodeRef) {
		return UrlUtil.getAlfrescoUrl(sysAdminParams) + webDavService.getWebdavUrl(nodeRef);
	}
	public String getShareUrl(NodeRef nodeRef) {
		SiteInfo site = siteService.getSite(nodeRef);
		
		if (fileFolderService.getFileInfo(nodeRef).isFolder()) {
			return UrlUtil.getShareUrl(sysAdminParams) + "/page/" 
					+ ((site != null) ? "site/" + site.getShortName() + "/documentlibrary" : "/repository")
					+ "#filter=path%7C%2FDirection%2520Achat%2FRecettes%7C&page=1";
		} else {
			return UrlUtil.getShareUrl(sysAdminParams) + "/page/" 
				+ ((site != null) ? "site/" + site.getShortName() : "")
				+ "/document-details?nodeRef=" + nodeRef;
		}
	}
	
	public String getDownloadUrl(NodeRef nodeRef, QName property) {
		// TODO manage properties other than cm:content
		return UrlUtil.getAlfrescoUrl(sysAdminParams) + "/s/api/node/" 
			+ nodeRef.getStoreRef().getProtocol() + "/" + nodeRef.getStoreRef().getIdentifier() + "/" 
			+ nodeRef.getId() + "/content";
			//+ ((rendition != null) ? "/thumbnails/" + rendition : "");
	}
}