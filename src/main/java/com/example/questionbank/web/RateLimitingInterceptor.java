package com.example.questionbank.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting interceptor for login and registration endpoints.
 * Limits requests to 5 attempts per minute per IP address.
 */
@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 5;
    private static final long TIME_WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RequestBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Only apply rate limiting to POST requests on auth endpoints
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!path.equals("/login") && !path.equals("/register") &&
            !path.equals("/forgot-password") && !path.equals("/verify-email/resend")) {
            return true;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + path;

        RequestBucket bucket = buckets.computeIfAbsent(key, k -> new RequestBucket());

        synchronized (bucket) {
            long now = System.currentTimeMillis();

            // Reset if time window has passed
            if (now - bucket.windowStart > TIME_WINDOW_MS) {
                bucket.windowStart = now;
                bucket.count = 0;
            }

            if (bucket.count >= MAX_REQUESTS) {
                long retryAfter = (bucket.windowStart + TIME_WINDOW_MS - now) / 1000;
                response.setStatus(429); // Too Many Requests
                response.setHeader("Retry-After", String.valueOf(retryAfter));
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again in " + retryAfter + " seconds.\"}");
                return false;
            }

            bucket.count++;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestBucket {
        long windowStart = System.currentTimeMillis();
        int count = 0;
    }
}
