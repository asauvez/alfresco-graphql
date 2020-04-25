package fr.smile.alfresco.graphql.unittest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.smile.alfresco.graphql.helper.PredicateHelper;
import fr.smile.alfresco.graphql.servlet.GraphQlConfigurationHelper;


public class PredicateHelperTest {
	
	private ObjectMapper mapper = new ObjectMapper();
	private NamespaceMap namespaceService;
	
	@Before
	public void init() {
		mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		
		namespaceService = new NamespaceMap();
		namespaceService.map("cm", "http://www.alfresco.org/model/content/1.0");
		namespaceService.map("exif", "http://www.alfresco.org/model/exif/1.0");
		
		GraphQlConfigurationHelper.namespaceService = namespaceService;
	}
	
	@Test
	public void testPredicateHelper() throws Exception {
		assertTransformation(
				"TYPE:{http://www.alfresco.org/model/content/1.0}content", 
				"[{ type: \"cm:content\" }]");
		assertTransformation(
				"=cm:name:\"readme.ftl\"", 
				"[{ eq: { property:\"cm:name\", value:\"readme.ftl\"} }]");
		assertTransformation(
				"=exif:pixelXDimension:300", 
				"[{ eqInt: { property:\"exif:pixelXDimension\", value:300 } }]");

		assertTransformation(
				"=cm:isIndexed:true", 
				"[{ isTrue: \"cm:isIndexed\" }]");
		assertTransformation(
				"=cm:isIndexed:false", 
				"[{ isFalse: \"cm:isIndexed\" }]");
		
		assertTransformation(
				"cm:created:[\"2020-01-01\" TO MAX]", 
				"[{ range: { property:\"cm:created\", min:\"2020-01-01\"} }]");
		assertTransformation(
				"cm:created:[MIN TO \"2020-01-01\"]", 
				"[{ range: { property:\"cm:created\", max:\"2020-01-01\"} }]");
		assertTransformation(
				"cm:created:<\"2019-01-01\" TO \"2020-01-01\">", 
				"[{ range: { property:\"cm:created\", min:\"2019-01-01\", max:\"2020-01-01\", minInclusive: false, maxInclusive: false} }]");
		assertTransformation(
				"exif:pixelXDimension:[100 TO 200]", 
				"[{ rangeInt: { property:\"exif:pixelXDimension\", min:100, max:200 } }]");

		assertTransformation(
				"NOT (@cm:name:\"readme.ftl\")", 
				"[{ match: { property:\"cm:name\", value:\"readme.ftl\"}, not: true }]");
		assertTransformation(
				"(A) AND (B)", 
				"[{ and: [{natif: \"A\"}, {natif: \"B\"}] }]");
		assertTransformation(
				"NOT ((A) OR (B))", 
				"[{ or: [{natif: \"A\"}, {natif: \"B\"}], not: true }]");
	}
	
	@SuppressWarnings("unchecked")
	private void assertTransformation(String expectedFts, String jsonQuery) throws Exception {
		List<Map<String, Object>> predicates = mapper.readValue(jsonQuery, List.class);
		String fts = PredicateHelper.getQuery(namespaceService, predicates);
		Assert.assertEquals(expectedFts, fts);
	}
	
	private static class NamespaceMap implements NamespacePrefixResolver {
		private Map<String, Set<String>> uriToPrefixesMap = new TreeMap<String, Set<String>>();
		private Map<String, String> prefixToUriMap = new TreeMap<String, String>();

		public void map(String prefix, String uri) {
			Set<String> prefixes = uriToPrefixesMap.get(uri);
			if (prefixes == null) {
				prefixes = new TreeSet<String>();
				uriToPrefixesMap.put(uri, prefixes);
			}
			prefixes.add(prefix);
			prefixToUriMap.put(prefix, uri);
		}

		public Collection<String> getURIs() {
			return uriToPrefixesMap.keySet();
		}

		public Collection<String> getPrefixes() {
			return prefixToUriMap.keySet();
		}

		public Collection<String> getPrefixes(String namespaceURI) throws NamespaceException {
			return Collections.unmodifiableCollection(uriToPrefixesMap.get(namespaceURI));
		}

		public String getNamespaceURI(String prefix) throws NamespaceException {
			return prefixToUriMap.get(prefix);
		}
	}
}