package fr.smile.alfresco.graphql.ws;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import fr.smile.alfresco.graphql.service.GraphQlService;

public class GraphQlWebScript extends AbstractWebScript {
	// private static Log logger = LogFactory.getLog(GraphQlWebScript.class);

	@Autowired
	private GraphQlService graphQlService;
	
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String query = req.getParameter("query");
		graphQlService.execute(query);
	}
}