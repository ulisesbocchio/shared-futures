package com.ulisesbocchio.sharedfutures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * @author ulisesbocchio
 */
public class SharedFuturesRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SharedFuturesRegistry.class);

    public SharedFuturesRegistry() {
        LOG.debug("Initialized Shared Futures Registry");
    }

    Map<String, Future<?>> registry = new ConcurrentHashMap<>();

    public Future<?> get(String key) {
        return registry.get(key);
    }

    public Future<?> put(String key, Future<?> future) {
        return registry.put(key, future);
    }

    public Future<?> remove(String key) {
        return registry.remove(key);
    }

    public boolean contains(String key) {
        return registry.containsKey(key);
    }

}
