package in.feekeeper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.feekeeper.auth.AuthService;
import in.feekeeper.auth.dto.AuthResponse;
import in.feekeeper.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class MemberCsvImportIntegrationTest {
    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(100_000_000);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("feekeeper_csv_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "csv-import-integration-secret-at-least-32-bytes");
    }

    @Autowired MockMvc mvc;
    @Autowired AuthService authService;
    @Autowired ObjectMapper json;

    @Test
    void importsTwoHundredMembersInOneRequest() throws Exception {
        String token = bearer(register("CSV 200 Gym"));
        StringBuilder csv = new StringBuilder(header());
        for (int index = 0; index < 200; index++) {
            csv.append("Member ").append(index).append(',').append(nextPhone())
                    .append(",Monthly,700.00,30,2026-11-01\n");
        }

        MockMultipartFile file = csvFile(csv.toString());
        mvc.perform(multipart("/api/members/import").file(file).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(200))
                .andExpect(jsonPath("$.importedCount").value(200))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.errors.length()").value(0));

        mvc.perform(get("/api/members").header("Authorization", token).param("page", "0").param("size", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(200))
                .andExpect(jsonPath("$.content.length()").value(200));
    }

    @Test
    void reportsInvalidAndDuplicateRowsAndDoesNotReadOtherTenantMembers() throws Exception {
        String tokenA = bearer(register("CSV Tenant A"));
        String tokenB = bearer(register("CSV Tenant B"));
        String phoneInB = nextPhone();
        var memberB = mvc.perform(post("/api/members").header("Authorization", tokenB)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Member B","phone":"%s","planName":"Monthly","planAmount":700.00,"planDurationDays":30,"joinDate":"2026-11-01"}
                                """.formatted(phoneInB)))
                .andExpect(status().isCreated()).andReturn();
        String memberBId = json.readTree(memberB.getResponse().getContentAsString()).get("id").asText();

        String importedName = "Tenant A imported member";
        String csv = header()
                + importedName + "," + phoneInB + ",Monthly,700.00,30,2026-11-01\n"
                + "Duplicate phone," + phoneInB + ",Monthly,700.00,30,2026-11-01\n"
                + "Bad phone,123,Monthly,700.00,30,2026-11-01\n";
        mvc.perform(multipart("/api/members/import").file(csvFile(csv)).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.errors.length()").value(2));

        mvc.perform(get("/api/members").header("Authorization", tokenA).param("search", importedName))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/members").header("Authorization", tokenB).param("search", importedName))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/members/{id}", memberBId).header("Authorization", tokenA))
                .andExpect(status().isNotFound());
    }

    private MockMultipartFile csvFile(String csv) {
        return new MockMultipartFile("file", "members.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
    }

    private String header() { return "fullName,phone,planName,planAmount,planDurationDays,joinDate\n"; }
    private String nextPhone() { return "7%09d".formatted(PHONE_SEQUENCE.getAndIncrement()); }

    private AuthResponse register(String businessName) {
        int sequence = PHONE_SEQUENCE.getAndIncrement();
        return authService.register(new RegisterRequest(businessName, "Owner", "8%09d".formatted(sequence), null,
                "owner-" + UUID.randomUUID() + "@example.com", "password123"));
    }

    private String bearer(AuthResponse response) { return "Bearer " + response.accessToken(); }
}
