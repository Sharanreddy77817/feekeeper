package in.feekeeper;

import com.fasterxml.jackson.databind.*;
import in.feekeeper.auth.AuthService;
import in.feekeeper.auth.dto.*;
import in.feekeeper.notification.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class Phase3IntegrationTest {
    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(500);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("feekeeper_phase3_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "phase-three-integration-secret-at-least-32-bytes");
        registry.add("app.whatsapp.provider", () -> "mock");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService authService;
    @Autowired DailyReminderJob job;
    @MockBean DailyReminderService reminderService;

    @Test
    void whatsappLinkEndpointIsTenantScopedAndReturnsPrefilledUrl() throws Exception {
        AuthResponse tenantA = register("A Gym");
        AuthResponse tenantB = register("B Gym");
        String tokenA = bearer(tenantA);
        String tokenB = bearer(tenantB);
        MvcResult created = mvc.perform(post("/api/members").header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(memberBody("B Member", nextPhone())))
                .andExpect(status().isCreated()).andReturn();
        String memberId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/members/{id}/whatsapp-link", memberId).header("Authorization", tokenA))
                .andExpect(status().isNotFound());
        MvcResult linkResult = mvc.perform(get("/api/members/{id}/whatsapp-link", memberId)
                        .header("Authorization", tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").exists()).andReturn();

        String url = json.readTree(linkResult.getResponse().getContentAsString()).get("url").asText();
        assertThat(url).startsWith("https://wa.me/91").contains("?text=");
    }

    @Test
    void shedLockAllowsOnlyOneConcurrentJobExecution() throws Exception {
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        doAnswer(invocation -> {
            firstEntered.countDown();
            releaseFirst.await(10, TimeUnit.SECONDS);
            return null;
        }).when(reminderService).run();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(job::runDaily);
            assertThat(firstEntered.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(job::runDaily);
            second.get(10, TimeUnit.SECONDS);
            verify(reminderService, times(1)).run();
            releaseFirst.countDown();
            first.get(10, TimeUnit.SECONDS);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private AuthResponse register(String businessName) {
        int sequence = PHONE_SEQUENCE.getAndIncrement();
        return authService.register(new RegisterRequest(businessName, "Owner", "8%09d".formatted(sequence),
                "business@upi", "owner-" + UUID.randomUUID() + "@example.com", "password123"));
    }

    private String memberBody(String name, String phone) throws Exception {
        return json.writeValueAsString(Map.of("fullName", name, "phone", phone, "planName", "Monthly",
                "planAmount", 1000.00, "planDurationDays", 30, "joinDate", LocalDate.now().toString()));
    }

    private String nextPhone() { return "9%09d".formatted(PHONE_SEQUENCE.getAndIncrement()); }
    private String bearer(AuthResponse response) { return "Bearer " + response.accessToken(); }
}
