package com.openmind.hagap.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * MVC interceptor for rate limiting — chose {@link HandlerInterceptor} over a servlet Filter because
 * interceptors can be scoped to specific URL patterns via WebMvcConfig (only {@code /api/chat/**}),
 * whereas Filters apply globally and need URL matching logic inside doFilter.
 *
 * <p>Security notes:
 * <ul>
 *   <li>{@code X-Forwarded-For} trusts the first entry (client IP). In production behind a trusted
 *       reverse proxy (Caddy/Nginx), this is correct. Without a trusted proxy, this header is
 *       spoofable — would need {@code spring.web.forwarded-headers-strategy=FRAMEWORK} for validation.</li>
 *   <li>preHandle returns false to short-circuit — the request never reaches the controller or
 *       any downstream filters. This is the cheapest rejection point in the Spring MVC pipeline.</li>
 *   <li>JSON error body (not plain text) so API clients can parse it consistently.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    /** Short-circuits with 429 before the request reaches the controller — minimal wasted work. */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        if (!rateLimiterService.tryConsumeByIp(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            response.setContentType("application/json");
            return false;
        }
        return true;
    }

    /** First entry in X-Forwarded-For = original client IP (when behind a trusted proxy). */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
