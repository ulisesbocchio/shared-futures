package com.ulisesbocchio.sharedfutures.spring;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @ulisesbocchio
 */
public class DefaultKeyGenerator implements KeyGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultKeyGenerator.class);

    public DefaultKeyGenerator() {
        LOG.debug("Initialized Default Key Generator");
    }

    @Override
    public String generate(Object target, Method method, Object... args) {
        Parameter[] params = method.getParameters();
        String key = IntStream.range(0, params.length)
                .mapToObj(i -> getParamKey(args[i], target.getClass(), method, params[i]))
                .limit(params.length)
                .collect(Collectors.joining(",", target.getClass().getName() + "#" + method.getName() + "(", ")"));
        LOG.debug("generated key: {}", key);
        return toMd5(key);
    }

    private String toMd5(String key) {
        return DigestUtils.md5Hex(key);
    }

    private String getParamKey(Object arg, Class targetClass, Method method, Parameter param) {
        if (arg instanceof SharedFutureKey) {
            return ((SharedFutureKey) arg).getKey();
        } else if (arg instanceof String) {
            return (String) arg;
        } else if (ClassUtils.isPrimitiveOrWrapper(arg.getClass())) {
            return arg.toString();
        } else if (arg instanceof Collection) {
            return getCollectionParam((Collection) arg, targetClass, method, param);
        } else if (arg instanceof Map) {
            return getCollectionParam(((Map) arg).entrySet(), targetClass, method, param);
        } else if (arg instanceof Map.Entry) {
            return getMapEntryParam((Map.Entry) arg, targetClass, method, param);
        }
        throw new IllegalArgumentException(
                String.format("Cannot generate key for type: %s found in Argument %s of method %s#%s",
                        arg.getClass().getName(), param.getName(), targetClass.getName(), method.getName()));
    }

    private String getMapEntryParam(Map.Entry arg, Class targetClass, Method method, Parameter param) {
        return getParamKey(arg.getKey(), targetClass, method, param) +
                "=" +
                getParamKey(arg.getValue(), targetClass, method, param);
    }

    private <T> String getCollectionParam(Collection<T> col, Class targetClass, Method method, Parameter param) {
        return col.stream()
                .map(e -> getParamKey(e, targetClass, method, param))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
