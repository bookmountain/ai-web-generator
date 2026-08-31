package com.book.aiwebgenerator.ratelimiter.enums;

public enum RateLimitType {

    /**
     * API-level rate limiting
     */
    API,

    /**
     * User-level rate limiting
     */
    USER,

    /**
     * IP-level rate limiting
     */
    IP
}