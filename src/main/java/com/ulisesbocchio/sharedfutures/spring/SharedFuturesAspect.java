package com.ulisesbocchio.sharedfutures.spring;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.ulisesbocchio.sharedfutures.SharedFuturesRegistry;
import com.ulisesbocchio.sharedfutures.spring.annotation.SharedFuture;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.concurrent.ListenableFuture;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @ulisesbocchio
 */
@Aspect
public class SharedFuturesAspect {

    private static final Logger LOG = LoggerFactory.getLogger(SharedFuturesAspect.class);

    private SharedFuturesRegistry registry;
    private KeyGenerator keyGenerator;
    private ExpressionParser elParser = new SpelExpressionParser();

    public SharedFuturesAspect(SharedFuturesRegistry registry, KeyGenerator keyGenerator) {
        this.registry = registry;
        this.keyGenerator = keyGenerator;
        LOG.debug("Initialized Shared Futures Aspect");
    }

    @Pointcut("execution(public java.util.concurrent.CompletableFuture *(..))")
    private void returnsCompletableFuture(){}

    @Pointcut("execution(public com.google.common.util.concurrent.ListenableFuture *(..))")
    private void returnsGuavaListenableFuture(){}

    @Pointcut("execution(public org.springframework.util.concurrent.ListenableFuture *(..))")
    private void returnsSpringListenableFuture(){}

    @Around("@annotation(annotation) && (returnsCompletableFuture() || returnsGuavaListenableFuture() || returnsSpringListenableFuture())")
    public Future<?> aroundSharedFutures(ProceedingJoinPoint pjp, SharedFuture annotation) throws Throwable {
        LOG.debug("entering shared futures aspect");
        Object target = pjp.getTarget();
        Method method = ((MethodSignature)pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();
        String key = getOrGenerateKey(annotation, target, method, args);
        LOG.debug("Generated key: {} from: {}", key, annotation.key());
        Future<?> sharedFuture = registry.get(key);
        if(sharedFuture == null) {
            LOG.debug("Storing future for shared used with key: {}", key);
            sharedFuture = (Future<?>) pjp.proceed();
            registry.put(key, subscribeForRemoval(sharedFuture, () -> {
                registry.remove(key);
                LOG.debug("Removing Shared Future after completion with key: {}", key);}));
        } else {
            LOG.debug("Found existing Shared Future in registry with key: {}", key);
        }
        return sharedFuture;
    }

    private <T> Future<T> subscribeForRemoval(Future<T> sharedFuture, Runnable removeFunc) {
        if (sharedFuture instanceof CompletableFuture) {
            return getRemovingCompletableFuture((CompletableFuture<T>) sharedFuture, removeFunc);
        } else if (sharedFuture instanceof ListenableFuture) {
            return getRemovingListenableFuture((ListenableFuture<T>) sharedFuture, removeFunc);
        } else if (sharedFuture instanceof com.google.common.util.concurrent.ListenableFuture) {
            return getRemovingGuavaFuture((com.google.common.util.concurrent.ListenableFuture<T>) sharedFuture, removeFunc);
        }
        throw new IllegalStateException("Invalid Future type: " + sharedFuture.getClass().getName());
    }

    private <T> Future<T> getRemovingGuavaFuture(com.google.common.util.concurrent.ListenableFuture<T> sharedFuture, final Runnable removeFunc) {
        sharedFuture.addListener(removeFunc, MoreExecutors.directExecutor());
        return sharedFuture;
    }

    private <T> Future<T> getRemovingListenableFuture(ListenableFuture<T> sharedFuture, Runnable removeFunc) {
        sharedFuture.addCallback(t -> removeFunc.run(), e -> removeFunc.run());
        return sharedFuture;
    }

    private <T> CompletableFuture<T> getRemovingCompletableFuture(CompletableFuture<T> sharedFuture, Runnable removeFunc) {
        return sharedFuture.whenComplete((t, e) -> removeFunc.run());
    }

    private String getOrGenerateKey(SharedFuture annotation, Object target, Method method, Object[] args) {
        return Optional.of(annotation)
                .map(SharedFuture::key)
                .filter(key -> !key.trim().isEmpty())
                .map(key -> this.getKeyFromAnnotation(key, target, method, args))
                .orElseGet(() -> getKeyFromGenerator(target, method, args));
    }

    private String getKeyFromAnnotation(String key, Object target, Method method, Object[] args) {
       return Optional.of(key)
                .filter(k -> !(k.startsWith("#{") && k.endsWith("}")))
                .orElseGet(() -> getKeyFromExpression(key, target, method, args));
    }

    private String getKeyFromExpression(String expr, Object target, Method method, Object[] args) {
        String expStr = expr.substring(2, expr.length()-1);
        Expression exp = elParser.parseExpression(expStr);
        EvaluationContext ctx = new StandardEvaluationContext(target);
        populateELContext(ctx, method, args);
        return exp.getValue(ctx, String.class);
    }

    private void populateELContext(EvaluationContext ctx, Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        for(int i = 0; i < params.length; i++) {
            ctx.setVariable(params[i].getName(), args[i]);
        }
    }

    private String getKeyFromGenerator(Object target, Method method, Object[] args) {
        return keyGenerator.generate(target, method, args);
    }
}
