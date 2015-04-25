package com.ulisesbocchio.sharedfutures;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

/**
 * @author ulisesbocchio
 */
public class SharedFuturesRegistryTest {

    @Test
    public void testPut() {
        SharedFuturesRegistry registry = new SharedFuturesRegistry();
        CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
        registry.put("key", future);
        Assert.assertTrue(registry.contains("key"));
    }

    @Test
    public void testGet() {
        SharedFuturesRegistry registry = new SharedFuturesRegistry();
        CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
        registry.put("key", future);
        Assert.assertEquals(future, registry.get("key"));
    }

    @Test
    public void testRemove() {
        SharedFuturesRegistry registry = new SharedFuturesRegistry();
        CompletableFuture<Object> future = CompletableFuture.completedFuture(null);
        registry.put("key", future);
        Assert.assertTrue(registry.contains("key"));
        registry.remove("key");
        Assert.assertFalse(registry.contains("key"));
    }
}
