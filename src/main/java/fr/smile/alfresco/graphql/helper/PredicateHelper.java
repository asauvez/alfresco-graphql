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
		
		String typePredicate = (String) predicate.get("type");
		if (typePredicate != null) {
			buf.append("TYPE:").append(getQName(typePredicate));
			nbOperator ++;
		}
		String exactTypePredicate = (String) predicate.get("exactType");
		if (exactTypePredicate != null) {
			buf.append("EXACTTYPE:").append(getQName(exactTypePredicate));
			nbOperator ++;
		}
		String aspectPredicate = (String) predicate.get("aspect");
		if (aspectPredicate != null) {
			buf.append("ASPECT:").append(getQName(aspectPredicate));
			nbOperator ++;
		}
		String exactAspectPredicate = (String) predicate.get("exactAspect");
		if (exactAspectPredicate != null) {
			buf.append("EXACTASPECT:").append(getQName(exactAspectPredicate));
			nbOperator ++;
		}

		String natifPredicate = (String) predicate.get("natif");
		if (natifPredicate != null) {
			buf.append(natifPredicate);
			nbOperator ++;
		}

		Map<String, Object> matchPredicate = (Map<String, Object>) predicate.get("match");
		if (matchPredicate != null) {
			String property = (String) matchPredicate.get("property");
			String value = (String) matchPredicate.get("value");
			buf.append("@").append(getQName(property).toPrefixString(namespaceService))
				.append(":").append(toFtsValue(value));
			nbOperator ++;
		}

		for (String valueType : new String[] { "", "Int" }) {
			Map<String, Object> eqPredicate = (Map<String, Object>) predicate.get("eq" + valueType);
			if (eqPredicate != null) {
				String property = (String) eqPredicate.get("property");
				Object value = eqPredicate.get("value");
				buf.append("=").append(getQName(property).toPrefixString(namespaceService))
					.append(":").append(toFtsValue(value));
				nbOperator ++;
			}
			Map<String, Object> rangePredicate = (Map<String, Object>) predicate.get("range" + valueType);
			if (rangePredicate != null) {
				String property = (String) rangePredicate.get("property");
				Object min = rangePredicate.get("min");
				Object max = rangePredicate.get("max");
				boolean minInclusive = (Boolean) rangePredicate.getOrDefault("minInclusive", Boolean.TRUE);
				boolean maxInclusive = (Boolean) rangePredicate.getOrDefault("maxInclusive", Boolean.TRUE);
				
				buf.append(getQName(property).toPrefixString(namespaceService))
					.append(minInclusive ? ":[" : ":<")
					.append((min != null) ? toFtsValue(min): "MIN")
					.append(" TO ")
					.append((max != null) ? toFtsValue(max): "MAX")
					.append(maxInclusive ? "]" : ">");
				
				nbOperator ++;
			}
		} 
		String isTruePredicate = (String) predicate.get("isTrue");
		if (isTruePredicate != null) {
			buf.append("=").append(getQName(isTruePredicate).toPrefixString(namespaceService))
				.append(":true");
			nbOperator ++;
		}
		String isFalsePredicate = (String) predicate.get("isFalse");
		if (isFalsePredicate != null) {
			buf.append("=").append(getQName(isFalsePredicate).toPrefixString(namespaceService))
				.append(":false");
			nbOperator ++;
		}
		
		if (not) {
			buf.append(")");
		}

		if (nbOperator != 1) {
			throw new IllegalArgumentException("There should be exactly one operator but got " + predicate);
		}
	}
		
	private QName getQName(String name) {
		return GraphQlConfigurationHelper.getQName(name);
	}
	private String toFtsValue(Object value) {
		if (value instanceof Number) {
			return value.toString();
		} else if (value instanceof String) {
			return "\"" + value.toString().replace("\"", "\\\"").replace("\n", "\\n") + "\"";
		} else {
			throw new IllegalStateException(value.getClass().toString());
		}
	}
}