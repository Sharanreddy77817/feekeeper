package in.feekeeper.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final AuthRateLimitProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(AuthRateLimitProperties properties, Clock clock, ObjectMapper objectMapper) {
        this.properties = properties; this.clock = clock; this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) ||
                !(request.getRequestURI().equals("/api/auth/register") || request.getRequestURI().equals("/api/auth/login"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Instant now = clock.instant();
        String key = clientIp(request) + ':' + request.getRequestURI();
        Window window = windows.compute(key, (ignored, existing) ->
                existing == null || now.isAfter(existing.startedAt.plus(Duration.ofMinutes(properties.windowMinutes())))
                        ? new Window(now, 1) : new Window(existing.startedAt, existing.count + 1));
        if (window.count > properties.attempts()) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "timestamp", now, "status", 429, "errorCode", "AUTH_RATE_LIMITED",
                    "message", "Too many authentication attempts", "path", request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) { return request.getRemoteAddr(); }
    private record Window(Instant startedAt, int count) { }
}
