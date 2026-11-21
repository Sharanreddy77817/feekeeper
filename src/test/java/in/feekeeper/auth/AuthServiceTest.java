package in.feekeeper.auth;

import in.feekeeper.auth.dto.*;
import in.feekeeper.common.exception.ConflictException;
import in.feekeeper.tenant.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private TenantRepository tenants;
    private UserRepository users;
    private PasswordEncoder encoder;
    private AuthService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T06:00:00Z"), ZoneOffset.UTC);

    @BeforeEach void setUp() {
        tenants = mock(TenantRepository.class); users = mock(UserRepository.class); encoder = mock(PasswordEncoder.class);
        when(encoder.encode("FeeKeeper timing equalization value")).thenReturn("dummy-hash");
        JwtService jwt = new JwtService(new JwtProperties("test-secret-that-is-definitely-32-bytes-long", 60), clock);
        service = new AuthService(tenants, users, encoder, jwt, clock);
        when(tenants.save(any())).thenAnswer(i -> i.getArgument(0));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void registrationCreatesTenantAndOwnerWithEncodedPassword() {
        when(encoder.encode("password123")).thenReturn("encoded");
        AuthResponse response = service.register(new RegisterRequest("Shakti Gym", "Asha", "9876543210", null,
                " OWNER@Example.com ", "password123"));
        ArgumentCaptor<AppUser> user = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(user.capture());
        assertThat(user.getValue().getEmail()).isEqualTo("owner@example.com");
        assertThat(user.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(user.getValue().getRole()).isEqualTo(UserRole.OWNER);
        assertThat(response.tenantId()).isEqualTo(user.getValue().getTenantId());
    }

    @Test void registrationWithChosenReceiptThemePreservesGalaxyTheme() {
        when(encoder.encode("password123")).thenReturn("encoded");
        AuthResponse response = service.register(new RegisterRequest("Nebula Fitness", "Karan", "9876543210", null,
                "karan@nebula.com", "password123", "NEBULA"));
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenants).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getReceiptTheme()).isEqualTo("NEBULA");
        assertThat(response.tenantId()).isEqualTo(tenantCaptor.getValue().getId());
    }

    @Test void registrationRejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("Gym", "Owner", "9876543210", null,
                "owner@example.com", "password123"))).isInstanceOf(ConflictException.class);
        verifyNoInteractions(tenants);
    }

    @Test void loginReturnsTokenForMatchingPassword() {
        AppUser user = new AppUser(UUID.randomUUID(), "owner@example.com", "encoded", UserRole.OWNER, clock.instant());
        when(users.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "encoded")).thenReturn(true);
        AuthResponse response = service.login(new LoginRequest("OWNER@example.com", "password123"));
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.accessToken()).isNotBlank();
    }

    @Test void loginUsesGenericFailureForUnknownEmailAndWrongPassword() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("missing@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class).hasMessage("Invalid email or password");
        verify(encoder).matches("wrong", "dummy-hash");
    }
}
