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
		boolean not = (Boolean) predicate.getOrDefault("not", Boolean.FALSE);
		if (not) {
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
		
		String type = (String) predicate.get("type");
		if (type != null) {
			buf.append("TYPE:").append(getQName(type));
			nbOperator ++;
		}
		String exactType = (String) predicate.get("exactType");
		if (exactType != null) {
			buf.append("EXACTTYPE:").append(getQName(exactType));
			nbOperator ++;
		}
		String aspect = (String) predicate.get("aspect");
		if (aspect != null) {
			buf.append("ASPECT:").append(getQName(aspect));
			nbOperator ++;
		}
		String exactAspect = (String) predicate.get("exactAspect");
		if (exactAspect != null) {
			buf.append("EXACTASPECT:").append(getQName(exactAspect));
			nbOperator ++;
		}

		String natif = (String) predicate.get("natif");
		if (natif != null) {
			buf.append(natif);
			nbOperator ++;
		}

		Map<String, Object> match = (Map<String, Object>) predicate.get("match");
		if (match != null) {
			String property = (String) match.get("property");
			String value = (String) match.get("value");
			buf.append("@").append(getQName(property).toPrefixString(namespaceService))
				.append(":").append(toFtsValue(match, value));
			nbOperator ++;
		}

		Map<String, Object> eq = (Map<String, Object>) predicate.get("eq");
		if (eq != null) {
			String property = (String) eq.get("property");
			String value = (String) eq.get("value");
			buf.append("=").append(getQName(property).toPrefixString(namespaceService))
				.append(":").append(toFtsValue(eq, value));
			nbOperator ++;
		}
		Map<String, Object> range = (Map<String, Object>) predicate.get("range");
		if (range != null) {
			String property = (String) range.get("property");
			String min = (String) range.get("min");
			String max = (String) range.get("max");
			boolean minInclusive = (Boolean) range.getOrDefault("minInclusive", Boolean.TRUE);
			boolean maxInclusive = (Boolean) range.getOrDefault("maxInclusive", Boolean.TRUE);
			
			buf.append(getQName(property).toPrefixString(namespaceService))
				.append(minInclusive ? ":[" : ":<")
				.append((min != null) ? toFtsValue(range, min): "MIN")
				.append(" TO ")
				.append((max != null) ? toFtsValue(range, max): "MAX")
				.append(maxInclusive ? "]" : ">");
			
			nbOperator ++;
		}
		
		if (not) {
			buf.append(")");
		}

		if (nbOperator != 1) {
			throw new IllegalArgumentException("There should be exactly one operator but got " + predicate);
		}
	}
	
	private enum PredicateValueType { STRING, NUMBER, BOOLEAN, DATE }
	
	private QName getQName(String name) {
		return GraphQlConfigurationHelper.getQName(name);
	}
	private String toFtsValue(Map<String, Object> map, String value) {
		PredicateValueType valueType = PredicateValueType.valueOf((String) map.getOrDefault("type", PredicateValueType.DATE.name()));
		switch (valueType) {
		case STRING:
		case DATE:
			return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
		case NUMBER:
		case BOOLEAN:
			return value;
		}
		throw new IllegalStateException(valueType.name());
	}
}