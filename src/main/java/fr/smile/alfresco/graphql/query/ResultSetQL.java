package fr.smile.alfresco.graphql.query;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.service.cmr.repository.NodeRef;

import fr.smile.alfresco.graphql.helper.QueryContext;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class ResultSetQL extends AbstractQLModel {

	private List<NodeRef> nodeRefs;
	private long numberFound;

	public ResultSetQL(QueryContext queryContext, List<NodeRef> nodeRefs, long numberFound) {
		super(queryContext);
		this.nodeRefs = nodeRefs;
		this.numberFound = numberFound;
	}

	public List<NodeQL> getNodes() {
		return nodeRefs.stream()
			.map(this::newNode)
			.collect(Collectors.toList());
	}
	
	public int getNumberFound() {
		return (int) numberFound;
	}

	public NodeQL getMin() {
		return getGroupProxy(BinaryOperator.minBy(Comparator.naturalOrder()));
	}
	public NodeQL getMax() {
		return getGroupProxy(BinaryOperator.maxBy(Comparator.naturalOrder()));
	}
	public NodeQL getSum() {
		return getGroupProxy((a, b) -> ((Number) a).intValue() + ((Number) b).intValue());
	}
	public NodeQL getAnd() {
		return getGroupProxy((a, b) -> ((Boolean) a) && ((Boolean) b));
	}
	public NodeQL getOr() {
		return getGroupProxy((a, b) -> ((Boolean) a) && ((Boolean) b));
	}

	private NodeQL getGroupProxy(BinaryOperator<?> operator) {
		List<NodeQL> nodes = getNodes();
		return getGroupProxy(NodeQL.class, nodes, operator);
	}

	@SuppressWarnings("unchecked")
	private <T extends AbstractQLModel> T getGroupProxy(Class<T> superClass, List<?> list, BinaryOperator<?> operator) {
		// TODO cache enhancer with Factory
		Enhancer e = new Enhancer();
		e.setClassLoader(getClass().getClassLoader());
		e.setSuperclass(superClass);
		e.setCallback(new MethodInterceptor() {
			public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
//				return proxy.invokeSuper(obj, args);
				return invoke(list, method, args, operator);
			}
		});
		Class<?>[] parameterTypes = superClass.getConstructors()[0].getParameterTypes();
		Object[] args = new Object[parameterTypes.length];
		return (T) e.create(parameterTypes, args);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object invoke(List list, Method method, Object[] args, BinaryOperator operator) {
		Stream<Object> stream = list.stream()
			.map(o -> {
				try {
					return method.invoke(o, args);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new IllegalStateException(e);
				}
			})
			.map(o -> ((o instanceof Optional) ? ((Optional) o).orElse(null) : o))
			.filter(o -> o != null);
		
		Class<?> returnType = method.getReturnType();
		if (Optional.class.isAssignableFrom(returnType)) {
			returnType = (Class) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
		}
		
		Object result = (AbstractQLModel.class.isAssignableFrom(returnType))
				? getGroupProxy((Class<AbstractQLModel>) returnType, stream.collect(Collectors.toList()), operator) 
				: stream.reduce(operator).orElse(null);
		
		if (Optional.class.isAssignableFrom(method.getReturnType())) {
			result = Optional.ofNullable(result);
		}
		return result;
	}	
}