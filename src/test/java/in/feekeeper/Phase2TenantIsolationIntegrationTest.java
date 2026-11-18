package in.feekeeper;

import com.fasterxml.jackson.databind.*;
import in.feekeeper.auth.AuthService;
import in.feekeeper.auth.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class Phase2TenantIsolationIntegrationTest {
    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(100);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("feekeeper_phase2_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "phase-two-integration-secret-at-least-32-bytes");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService authService;

    @Test
    void promotionCodesAreIsolatedByTenant() throws Exception {
        AuthResponse tenantA=register("Promo Tenant A"); AuthResponse tenantB=register("Promo Tenant B");
        String tokenA=bearer(tenantA), tokenB=bearer(tenantB); String code="BONLY"+UUID.randomUUID().toString().substring(0,8);
        String promo="{\"code\":\"%s\",\"name\":\"Private offer\",\"discountPercent\":10.00,\"validFrom\":\"%s\",\"validTo\":\"%s\"}".formatted(code,LocalDate.now().minusDays(1),LocalDate.now().plusDays(5));
        mvc.perform(post("/api/promotions").header("Authorization",tokenB).contentType(MediaType.APPLICATION_JSON).content(promo)).andExpect(status().isOk());
        mvc.perform(get("/api/promotions").header("Authorization",tokenA)).andExpect(status().isOk()).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(0)));
        mvc.perform(post("/api/promotions/validate").header("Authorization",tokenA).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"%s\",\"amount\":100.00,\"planDurationDays\":365}".formatted(code))).andExpect(status().isNotFound());
    }

    @Test
    void everyPhaseTwoEndpointKeepsTenantDataIsolated() throws Exception {
        AuthResponse tenantA = register("A Gym");
        AuthResponse tenantB = register("B Gym");
        String tokenA = bearer(tenantA);
        String tokenB = bearer(tenantB);
        String distinctiveName = "Only B " + UUID.randomUUID();
        String memberBPhone = nextPhone();
        JsonNode memberB = createMember(tokenB, distinctiveName, memberBPhone);
        String memberBId = memberB.get("id").asText();

        mvc.perform(post("/api/members").header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON).content(memberBody("Duplicate", memberBPhone, 30)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/members/{id}", memberBId).header("Authorization", tokenA))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/members/{id}", memberBId).header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(memberBody("Changed", nextPhone(), 30)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/members/{id}", memberBId).header("Authorization", tokenA))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/members").header("Authorization", tokenA).param("search", distinctiveName))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/members").header("Authorization", tokenA).param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));

        mvc.perform(post("/api/payments/mark-paid/{memberId}", memberBId)
                        .header("Authorization", tokenA).header("Idempotency-Key", "foreign-pay")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMode\":\"UPI\"}"))
                .andExpect(status().isNotFound());

        MvcResult paid = mvc.perform(post("/api/payments/mark-paid/{memberId}", memberBId)
                        .header("Authorization", tokenB).header("Idempotency-Key", "b-payment")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMode\":\"CASH\"}"))
                .andExpect(status().isOk()).andReturn();
        String paymentBId = json.readTree(paid.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/payments/mark-paid/{memberId}", memberBId)
                        .header("Authorization", tokenB).header("Idempotency-Key", "b-payment")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMode\":\"CASH\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(paymentBId));

        mvc.perform(get("/api/payments").header("Authorization", tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/payments").header("Authorization", tokenA).param("memberId", memberBId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(get("/api/payments/{id}/receipt.pdf", paymentBId).header("Authorization", tokenA))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/payments/{id}/receipt.pdf", paymentBId).header("Authorization", tokenB))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mvc.perform(get("/api/dashboard").header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveMembers").value(0))
                .andExpect(jsonPath("$.revenueThisMonth").value(0));
        mvc.perform(get("/api/dashboard").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveMembers").value(1))
                .andExpect(jsonPath("$.revenueThisMonth").value(1000.00));

        mvc.perform(put("/api/tenant/settings").header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"A Updated\",\"ownerName\":\"Owner A\",\"phone\":\"9876543201\",\"upiId\":\"a@bank\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.businessName").value("A Updated"));
        mvc.perform(get("/api/tenant/settings").header("Authorization", tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.businessName").value("B Gym"));

        mvc.perform(delete("/api/members/{id}", memberBId).header("Authorization", tokenB))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/members/{id}", memberBId).header("Authorization", tokenB))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/members").header("Authorization", tokenB).param("search", distinctiveName))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void concurrentPaymentsSerializeMemberRenewalAndProduceDistinctReceipts() throws Exception {
        AuthResponse tenant = register("Concurrent Gym");
        String token = bearer(tenant);
        JsonNode member = createMember(token, "Concurrent Member", nextPhone());
        String memberId = member.get("id").asText();
        LocalDate initialExpiry = LocalDate.parse(member.get("expiryDate").asText());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<MvcResult> first = executor.submit(() -> markPaidAfter(start, token, memberId, "parallel-1"));
            Future<MvcResult> second = executor.submit(() -> markPaidAfter(start, token, memberId, "parallel-2"));
            start.countDown();
            JsonNode one = json.readTree(first.get(30, TimeUnit.SECONDS).getResponse().getContentAsString());
            JsonNode two = json.readTree(second.get(30, TimeUnit.SECONDS).getResponse().getContentAsString());

            assertThat(one.get("receiptNumber").asText()).isNotEqualTo(two.get("receiptNumber").asText());
            assertThat(Set.of(one.get("newExpiryDate").asText(), two.get("newExpiryDate").asText()))
                    .contains(initialExpiry.plusDays(30).toString(), initialExpiry.plusDays(60).toString());
        } finally {
            executor.shutdownNow();
        }

        mvc.perform(get("/api/members/{id}", memberId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiryDate").value(initialExpiry.plusDays(60).toString()));
        mvc.perform(get("/api/payments").header("Authorization", token).param("memberId", memberId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }

    private MvcResult markPaidAfter(CountDownLatch start, String token, String memberId, String key) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        return mvc.perform(post("/api/payments/mark-paid/{memberId}", memberId)
                        .header("Authorization", token).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paymentMode\":\"UPI\"}"))
                .andExpect(status().isOk()).andReturn();
    }

    private AuthResponse register(String businessName) {
        int sequence = PHONE_SEQUENCE.getAndIncrement();
        return authService.register(new RegisterRequest(businessName, "Owner", "8%09d".formatted(sequence), null,
                "owner-" + UUID.randomUUID() + "@example.com", "password123"));
    }

    private JsonNode createMember(String token, String name, String phone) throws Exception {
        MvcResult result = mvc.perform(post("/api/members").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(memberBody(name, phone, 30)))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private String memberBody(String name, String phone, int duration) throws Exception {
        return json.writeValueAsString(Map.of(
                "fullName", name, "phone", phone, "planName", "Monthly",
                "planAmount", 1000.00, "planDurationDays", duration,
                "joinDate", LocalDate.now().toString()));
    }

    private String nextPhone() { return "9%09d".formatted(PHONE_SEQUENCE.getAndIncrement()); }
    private String bearer(AuthResponse response) { return "Bearer " + response.accessToken(); }
}
