package com.book.aiwebgenerator.ratelimiter;

import com.book.aiwebgenerator.ratelimiter.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Rate limit key prefix
     */
    String key() default "";

    /**
     * Number of allowed requests per time window
     */
    int rate() default 10;

    /**
     * Time window (in seconds)
     */
    int rateInterval() default 1;

    /**
     * Rate limit type
     */
    RateLimitType limitType() default RateLimitType.USER;

    /**
     * Rate limit message prompt
     */
    String message() default "Too many requests, please try again later";
}