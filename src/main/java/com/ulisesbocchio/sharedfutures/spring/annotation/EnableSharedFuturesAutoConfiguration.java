package com.ulisesbocchio.sharedfutures.spring.annotation;

import com.ulisesbocchio.sharedfutures.SharedFuturesRegistry;
import com.ulisesbocchio.sharedfutures.spring.DefaultKeyGenerator;
import com.ulisesbocchio.sharedfutures.spring.SharedFuturesAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @ulisesbocchio
 */
@Configuration
@EnableAspectJAutoProxy
public class EnableSharedFuturesAutoConfiguration {

    @Bean
    public SharedFuturesAspect sharedFuturesAspect() {
        return new SharedFuturesAspect(sharedFuturesRegistry(), defaultKeyGenerator());
    }

    @Bean
    public SharedFuturesRegistry sharedFuturesRegistry() {
        return new SharedFuturesRegistry();
    }

    @Bean
    public DefaultKeyGenerator defaultKeyGenerator() {
        return new DefaultKeyGenerator();
    }
}
