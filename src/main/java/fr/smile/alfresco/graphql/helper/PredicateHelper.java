package fr.smile.alfresco.graphql.helper;

import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;

public class PredicateHelper {

	private NamespacePrefixResolver namespaceService;

	public PredicateHelper(NamespacePrefixResolver namespaceService) {
		this.namespaceService = namespaceService;
	}

	public String getQuery(List<Map<String, Object>> predicates) {
		StringBuilder buf = new StringBuilder();
		parseBooleanOperator(buf, "AND", predicates);
		return buf.toString();
	}

	public void parseBooleanOperator(StringBuilder buf, String operator, List<Map<String, Object>> predicates) {
		boolean first = true;
		for (Map<String, Object> predicate : predicates) {
			if (first) {
				first = false;
			} else {
				buf.append(" ").append(operator).append(" ");
			}
			if (predicates.size() > 1) {
				buf.append("(");
			}
			parsePredicate(buf, predicate);
			if (predicates.size() > 1) {
				buf.append(")");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void parsePredicate(StringBuilder buf, Map<String, Object> predicate) {
		Boolean not = (Boolean) predicate.get("not");
		if (not != null && ! not.booleanValue()) {
			buf.append("NOT (");
		}
		
		int nbOperator = 0;
		List<Map<String, Object>> andPredicates = (List<Map<String, Object>>) predicate.get("and");
		if (andPredicates != null) {
			parseBooleanOperator(buf, "AND", andPredicates);
			nbOperator ++;
		}
		
		List<Map<String, Object>> orPredicates = (List<Map<String, Object>>) predicate.get("or");
		if (orPredicates != null) {
			parseBooleanOperator(buf, "OR", orPredicates);
			nbOperator ++;
		}
		
		String exactType = (String) predicate.get("exactType");
		if (exactType != null) {
			buf.append("EXACTTYPE:").append(getQName(exactType));
			nbOperator ++;
		}
		String type = (String) predicate.get("type");
		if (type != null) {
			buf.append("TYPE:").append(getQName(type));
			nbOperator ++;
		}
		String aspect = (String) predicate.get("aspect");
		if (aspect != null) {
			buf.append("ASPECT:").append(getQName(type));
			nbOperator ++;
		}
		String exactAspect = (String) predicate.get("exactAspect");
		if (exactAspect != null) {
			buf.append("EXACTASPECT:").append(getQName(type));
			nbOperator ++;
		}

		Map<String, Object> match = (Map<String, Object>) predicate.get("match");
		if (match != null) {
			String property = (String) match.get("property");
			Object value = match.get("value");
			buf.append("@").append(getQName(property).toPrefixString(namespaceService))
				.append(":").append(toFtsValue(value));
			nbOperator ++;
		}

		Map<String, Object> eq = (Map<String, Object>) predicate.get("eq");
		if (eq != null) {
			String property = (String) eq.get("property");
			Object value = eq.get("value");
			buf.append("=").append(getQName(property).toPrefixString(namespaceService))
				.append(":").append(toFtsValue(value));
			nbOperator ++;
		}
		if (not != null && ! not.booleanValue()) {
			buf.append(")");
		}

		if (nbOperator != 1) {
			throw new IllegalArgumentException("There should be exactly one operator but got " + predicate);
		}
	}
	
	private QName getQName(String name) {
		if (name.startsWith(String.valueOf(QName.NAMESPACE_BEGIN))) {
			return QName.createQName(name);
		} else {
			return QName.createQName(name, namespaceService);
		}
	}
	private String toFtsValue(Object value) {
		if (value instanceof String) {
			return "\"" + value.toString().replace("\"", "\\\"").replace("\n", "\\n") + "\"";
		} else {
			return value.toString();
		}
	}
}