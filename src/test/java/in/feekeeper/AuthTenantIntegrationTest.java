package in.feekeeper;

import in.feekeeper.auth.*;
import in.feekeeper.auth.dto.*;
import in.feekeeper.member.*;
import in.feekeeper.tenant.TenantFilterService;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class AuthTenantIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("feekeeper_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "integration-secret-that-is-at-least-32-bytes");
    }

    @Autowired AuthService authService;
    @Autowired JwtService jwtService;
    @Autowired MemberRepository memberRepository;
    @Autowired TenantFilterService tenantFilterService;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactions;

    @Test
    void registerThenLoginTokenContainsCorrectTenant() {
        String email = "owner-" + UUID.randomUUID() + "@example.com";
        AuthResponse registered = register(email, "9876543210");
        AuthResponse loggedIn = authService.login(new LoginRequest(email, "password123"));
        assertThat(jwtService.parse(loggedIn.accessToken()).tenantId()).isEqualTo(registered.tenantId());
        assertThat(jwtService.parse(loggedIn.accessToken()).userId()).isEqualTo(registered.userId());
    }

    @Test
    void explicitTenantPredicateAndHibernateFilterHideOtherTenant() {
        AuthResponse tenantA = register("a-" + UUID.randomUUID() + "@example.com", "9876543211");
        AuthResponse tenantB = register("b-" + UUID.randomUUID() + "@example.com", "9876543212");
        UUID memberBId = transactions.execute(status -> {
            Member memberB = new Member(tenantB.tenantId(), "B Member", "+919876543213", "Monthly",
                    new BigDecimal("1000.00"), 30, LocalDate.now(), LocalDate.now().plusDays(30),
                    MemberStatus.ACTIVE, Instant.now());
            return memberRepository.saveAndFlush(memberB).getId();
        });

        transactions.executeWithoutResult(status -> {
            assertThat(memberRepository.findByIdAndTenantIdAndDeletedAtIsNull(memberBId, tenantA.tenantId())).isEmpty();
            entityManager.clear();
            tenantFilterService.enableFor(tenantA.tenantId());
            assertThat(entityManager.unwrap(Session.class).get(Member.class, memberBId)).isNull();
        });
    }

    private AuthResponse register(String email, String phone) {
        return authService.register(new RegisterRequest("Test Gym", "Owner", phone, null, email, "password123"));
    }
}
