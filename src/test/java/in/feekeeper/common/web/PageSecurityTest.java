package in.feekeeper.common.web;

import in.feekeeper.auth.*;
import in.feekeeper.dashboard.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PageController.class, DashboardController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthRateLimitFilter.class, PageSecurityTest.Beans.class})
class PageSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean JwtService jwtService;
    @MockBean DashboardService dashboardService;
    @MockBean RequestLimitProperties requestLimitProperties;

    @Test void loginPageReturns200() throws Exception { mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login")); }
    @Test void registerPageReturns200() throws Exception { mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("register")); }
    @Test void dashboardPageReturns200() throws Exception { mvc.perform(get("/dashboard")).andExpect(status().isOk()).andExpect(view().name("dashboard")); }
    @Test void settingsPageReturns200() throws Exception { mvc.perform(get("/settings")).andExpect(status().isOk()).andExpect(view().name("settings")); }

    @Test
    void browserResponsesIncludeSecurityHeaders() throws Exception {
        mvc.perform(get("/login").secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", org.hamcrest.Matchers.containsString("max-age=31536000")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
    }

    @Test
    void protectedApiCallsFailWithoutJwt() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/members")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tenant/settings")).andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class Beans {
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean AuthRateLimitProperties authRateLimitProperties() { return new AuthRateLimitProperties(10, 1); }
        @Bean CorsProperties corsProperties() { return new CorsProperties(List.of("http://localhost:8080")); }
    }
}
