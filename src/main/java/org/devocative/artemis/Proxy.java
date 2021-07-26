package org.devocative.artemis;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class Proxy {
	private final Object target;

	// ------------------------------

	private Proxy(Object target) {
		this.target = target;
	}

	// ------------------------------

	public Object get() {
		final Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(target.getClass());
		enhancer.setCallback((MethodInterceptor) (obj, method, vars, proxy) -> {
			final Object result = proxy.invoke(target, vars);

			log.debug("{}.{} -> {}", target.getClass().getSimpleName(), method.getName(), result);

			if (result != null) {
				if (result instanceof List) {
					return ((List<?>) result).stream()
						.filter(Objects::nonNull)
						.map(item -> doProxify(item) ? create(item) : item)
						.collect(Collectors.toList());
				} else if (result instanceof String) {
					final String str = (String) result;
					return str.contains("${") ? ContextHandler.eval(str) : str;
				} else if (doProxify(result)) {
					return create(result);
				}
				return result;
			} else if (method.getReturnType().equals(List.class)) {
				return Collections.emptyList();
			}
			return null;
		});
		return enhancer.create();
	}

	// ------------------------------

	public static Object create(Object obj) {
		return new Proxy(obj).get();
	}

	// ------------------------------

	private boolean doProxify(Object obj) {
		return obj.getClass().isAnnotationPresent(XStreamAlias.class);
	}
}
