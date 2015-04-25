package com.ulisesbocchio.sharedfutures.spring;

import java.lang.reflect.Method;

/**
 * @author ulisesbocchio
 */
public interface KeyGenerator {

    String generate(Object target, Method method, Object... params);
}
