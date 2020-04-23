package fr.smile.alfresco.graphql.helper;

import java.util.List;
import java.util.Map;

import org.alfresco.service.namespace.NamespacePrefixResolver;

@SuppressWarnings("unchecked")
public class PredicateHelper {

	private NamespacePrefixResolver namespacePrefixResolver;

	private StringBuilder buf = new StringBuilder();

	private PredicateHelper(NamespacePrefixResolver namespacePrefixResolver) {
		this.namespacePrefixResolver = namespacePrefixResolver;
	}

	public static String getQuery(NamespacePrefixResolver namespacePrefixResolver, List<Map<String, Object>> predicates) {
		PredicateHelper helper = new PredicateHelper(namespacePrefixResolver);
		helper.parseBooleanOperator("AND", predicates);
		return helper.buf.toString();
	}

	private void parseBooleanOperator(String operator, List<Map<String, Object>> predicates) {
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

	private void parsePredicate(StringBuilder buf, Map<String, Object> predicate) {
		boolean not = (Boolean) predicate.getOrDefault("not", Boolean.FALSE);
		if (not) {
			buf.append("NOT (");
		}
		
		int nbOperator = 0;
		for (PredicateType type : PredicateType.values()) {
			Object value = predicate.get(type.name());
			if (value != null) {
				type.parse(this, value);
				nbOperator ++;
			}
		}
		if (nbOperator != 1) {
			throw new IllegalArgumentException("There should be exactly one operator but got " + predicate);
		}
		
		if (not) {
			buf.append(")");
		}
	}
	
	private enum PredicateType {
		and {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.parseBooleanOperator("AND", (List<Map<String, Object>>) detail);
			}
		},
		or {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.parseBooleanOperator("OR", (List<Map<String, Object>>) detail);
			}
		},

		type {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.append("TYPE:").appendFullyQualified((String) detail);
			}
		},
		exactType {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.append("EXACTTYPE:").appendFullyQualified((String) detail);
			}
		},
		aspect {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.append("ASPECT:").appendFullyQualified((String) detail);
			}
		},
		exactAspect {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.append("EXACTASPECT:").appendFullyQualified((String) detail);
			}
		},

		natif {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.append((String) detail);
			}
		},

		match {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.appendComparaison("@", (Map<String, Object>) detail);
			}
		},

		eq {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.appendComparaison("=", (Map<String, Object>) detail);
			}
		},
		eqInt {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.appendComparaison("=", (Map<String, Object>) detail);
			}
		},

		range {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.appendRange((Map<String, Object>) detail);
			}
		},
		rangeInt {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.appendRange((Map<String, Object>) detail);
			}
		},

		isTrue {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.append("=").appendPrefixString((String) detail).append(":true");
			}
		},
		isFalse {
			@Override public void parse(PredicateHelper helper, Object detail) {
				helper.append("=").appendPrefixString((String) detail).append(":false");
			}
		}
		;
		public abstract void parse(PredicateHelper helper, Object detail);

	}

	protected PredicateHelper append(String value) {
		buf.append(value);
		return this;
	}
	protected PredicateHelper appendFullyQualified(String property) {
		return append(GraphQlConfigurationHelper.getQName(property).toString());
	}
	protected PredicateHelper appendPrefixString(String property) {
		return append(GraphQlConfigurationHelper.getQName(property).toPrefixString(namespacePrefixResolver));
	}
	protected PredicateHelper appendFtsValue(Object value, String defaultValue) {
		if (value == null) {
			return append(defaultValue);
		} else if (value instanceof Number) {
			return append(value.toString());
		} else if (value instanceof String) {
			return append("\"").append(value.toString().replace("\"", "\\\"").replace("\n", "\\n")).append("\"");
		} else {
			throw new IllegalStateException(value.getClass().toString());
		}
	}
	
	protected PredicateHelper appendComparaison(String prefix, Map<String, Object> detail) {
		String property = (String) detail.get("property");
		Object value = detail.get("value");

		return append(prefix).appendPrefixString(property)
			.append(":").appendFtsValue(value, "");
	}
	
	protected PredicateHelper appendRange(Map<String, Object> detail) {
		String property = (String) detail.get("property");
		Object min = detail.get("min");
		Object max = detail.get("max");
		boolean minInclusive = (Boolean) detail.getOrDefault("minInclusive", Boolean.TRUE);
		boolean maxInclusive = (Boolean) detail.getOrDefault("maxInclusive", Boolean.TRUE);
		
		return appendPrefixString(property)
			.append(minInclusive ? ":[" : ":<")
			.appendFtsValue(min, "MIN")
			.append(" TO ")
			.appendFtsValue(max, "MAX")
			.append(maxInclusive ? "]" : ">");
	}
}