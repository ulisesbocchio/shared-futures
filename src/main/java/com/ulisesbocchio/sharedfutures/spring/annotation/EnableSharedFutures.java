package com.ulisesbocchio.sharedfutures.spring.annotation;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @ulisesbocchio
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Configuration
@Import({EnableSharedFuturesAutoConfiguration.class})
public @interface EnableSharedFutures {
}
