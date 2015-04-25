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
        StringBuilder key = new StringBuilder(target.getClass().getName());
        key.append("#").append(method.getName()).append("(");
        Parameter[] params = method.getParameters();
        for(int i = 0; i < args.length; i++) {
            key.append(getParamKey(args[i], target.getClass(), method, params[i]));
            if(i < args.length - 1) {
                key.append(",");
            }
        }
        key.append(")");
        LOG.debug("generated key: {}", key.toString());
        return toMd5(key.toString());
    }

    private String toMd5(String key) {
        return DigestUtils.md5Hex(key);
    }

    private String getParamKey(Object arg, Class targetClass, Method method, Parameter param) {
        if(arg instanceof SharedFutureKey) {
            return ((SharedFutureKey) arg).getKey();
        } else if (arg instanceof String) {
            return (String) arg;
        } else if (ClassUtils.isPrimitiveOrWrapper(arg.getClass())) {
            return  arg.toString();
        } else if (arg instanceof Collection) {
            return getCollectionParam((Collection)arg, targetClass, method, param);
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

    private String getCollectionParam(Collection col, Class targetClass, Method method, Parameter param) {
        StringBuilder key = new StringBuilder("[");
        Iterator it = col.iterator();
        while (it.hasNext()) {
            key.append(getParamKey(it.next(), targetClass, method, param));
            if(it.hasNext()) {
                key.append(",");
            }
        }
        key.append("]");
        return key.toString();
    }
}
