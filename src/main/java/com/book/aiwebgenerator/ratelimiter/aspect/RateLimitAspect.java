package com.book.aiwebgenerator.ratelimiter.aspect;

import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.model.entity.User;
import com.book.aiwebgenerator.ratelimiter.RateLimit;
import com.book.aiwebgenerator.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        String key = generateRateLimitKey(point, rateLimit);
        // Use Redisson's distributed rate limiter
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.expire(Duration.ofHours(1)); // Expire after 1 hour
        // Set rate limiter parameters: allowed requests per time window and the time window itself
        rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(), RateIntervalUnit.SECONDS);
        // Try to acquire a token; throw an exception if acquisition fails (rate limited)
        if (!rateLimiter.tryAcquire(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
    }

    private String generateRateLimitKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_limit:");
        // Append custom prefix
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        // Generate different keys based on the rate limit type
        switch (rateLimit.limitType()) {
            case API:
                // API-level: Method name
                MethodSignature signature = (MethodSignature) point.getSignature();
                Method method = signature.getMethod();
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName())
                        .append(".").append(method.getName());
                break;
            case USER:
                // User-level: User ID
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        User loginUser = userService.getLoginUser(request);
                        keyBuilder.append("user:").append(loginUser.getId());
                    } else {
                        // Request context unavailable, fallback to IP rate limiting
                        keyBuilder.append("ip:").append(getClientIP());
                    }
                } catch (BusinessException e) {
                    // Unauthenticated users fallback to IP rate limiting
                    keyBuilder.append("ip:").append(getClientIP());
                }
                break;
            case IP:
                // IP-level: Client IP
                keyBuilder.append("ip:").append(getClientIP());
                break;
            default:
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported rate limit type");
        }
        return keyBuilder.toString();
    }

    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multi-tier proxy scenarios (take the first IP in the comma-separated chain)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}