package com.ulisesbocchio.sharedfutures.spring;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.ulisesbocchio.sharedfutures.spring.annotation.EnableSharedFutures;
import com.ulisesbocchio.sharedfutures.spring.annotation.SharedFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ulisesbocchio
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SharedFuturesAspectTest.TestConfig.class)
public class SharedFuturesAspectTest {

    @Autowired
    private ISharedFuturesService service;

    @Before
    public void beforeTest(){
        service.reset();
    }

    @Test
    public void testSharedFuturesService() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> one = service.getInt();
        CompletableFuture<Integer> one2 = service.getInt();
        int oneInt = one.get();
        int one2Int = one2.get();
        Assert.assertEquals(1, oneInt);
        Assert.assertEquals(1, one2Int);
        Assert.assertEquals(oneInt, one2Int);
        CompletableFuture<Integer> two = service.getInt();
        CompletableFuture<Integer> two2 = service.getInt();
        int twoInt = two.get();
        int two2Int = two2.get();
        Assert.assertEquals(2, twoInt);
        Assert.assertEquals(2, two2Int);
    }

    @Test
    public void testSharedFuturesService_springListenable() throws ExecutionException, InterruptedException {
        ListenableFuture<Integer> one = service.getIntListenable();
        ListenableFuture<Integer> one2 = service.getIntListenable();
        int oneInt = one.get();
        int one2Int = one2.get();
        Assert.assertEquals(1, oneInt);
        Assert.assertEquals(1, one2Int);
        ListenableFuture<Integer> two = service.getIntListenable();
        ListenableFuture<Integer> two2 = service.getIntListenable();
        int twoInt = two.get();
        int two2Int = two2.get();
        Assert.assertEquals(2, twoInt);
        Assert.assertEquals(2, two2Int);
    }

    @Test
    public void testSharedFuturesService_guavaListenable() throws ExecutionException, InterruptedException {
        com.google.common.util.concurrent.ListenableFuture<Integer> one = service.getIntGuavaListenable();
        com.google.common.util.concurrent.ListenableFuture<Integer> one2 = service.getIntGuavaListenable();
        int oneInt = one.get();
        int one2Int = one2.get();
        Assert.assertEquals(1, oneInt);
        Assert.assertEquals(1, one2Int);
        Thread.sleep(1L);
        com.google.common.util.concurrent.ListenableFuture<Integer> two = service.getIntGuavaListenable();
        com.google.common.util.concurrent.ListenableFuture<Integer> two2 = service.getIntGuavaListenable();
        int twoInt = two.get();
        int two2Int = two2.get();
        Assert.assertEquals(2, twoInt);
        Assert.assertEquals(2, two2Int);
    }

    @Test
    public void testSharedFuturesService_withParams() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> one = service.getInt(10);
        CompletableFuture<Integer> one2 = service.getInt(10);
        CompletableFuture<Integer> two = service.getInt(100);
        CompletableFuture<Integer> two2 = service.getInt(100);
        int oneInt = one.get();
        int one2Int = one2.get();
        int twoInt = two.get();
        int two2Int = two2.get();
        Assert.assertEquals(11, oneInt);
        Assert.assertEquals(11, one2Int);
        Assert.assertEquals(102, twoInt);
        Assert.assertEquals(102, two2Int);
    }

    @Configuration
    @EnableAspectJAutoProxy
    @EnableSharedFutures
    public static class TestConfig {
        @Bean
        SharedFuturesService sharedFuturesService() {
            return new SharedFuturesService();
        }
    }

    public static class SharedFuturesService implements ISharedFuturesService, InitializingBean {

        private AtomicInteger i = new AtomicInteger(1);
        private ExecutorService eService = Executors.newFixedThreadPool(5);
        private ThreadPoolTaskExecutor springService = new ThreadPoolTaskExecutor();
        private ListeningExecutorService guavaService = MoreExecutors.listeningDecorator(eService);

        @Override
        public void afterPropertiesSet() throws Exception {
            springService.setCorePoolSize(5);
            springService.initialize();
        }

        @Override
        public void reset() {
            i.set(1);
        }

        private int getInternal() {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return i.getAndIncrement();
        }


        @SharedFuture
        @Override
        public CompletableFuture<Integer> getInt() {
            return CompletableFuture.supplyAsync(this::getInternal);
        }

        @SharedFuture
        @Override
        public CompletableFuture<Integer> getInt(int i) {
            return CompletableFuture.supplyAsync(() -> i + getInternal());
        }

        @SharedFuture
        @Override
        public ListenableFuture<Integer> getIntListenable() {
            return springService.submitListenable(this::getInternal);
        }

        @SharedFuture
        @Override
        public com.google.common.util.concurrent.ListenableFuture<Integer> getIntGuavaListenable() {
            return guavaService.submit(this::getInternal);
        }
    }

    public static interface ISharedFuturesService {
        void reset();
        CompletableFuture<Integer> getInt();
        CompletableFuture<Integer> getInt(int i);
        ListenableFuture<Integer> getIntListenable();
        com.google.common.util.concurrent.ListenableFuture<Integer> getIntGuavaListenable();
    }
}
