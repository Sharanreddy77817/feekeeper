package in.feekeeper.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-16T06:00:00Z"), ZoneOffset.UTC);
    private final JwtService service = new JwtService(new JwtProperties("test-secret-that-is-definitely-32-bytes-long", 60), clock);

    @Test void generatesAndParsesRequiredClaims() {
        UUID tenantId = UUID.randomUUID();
        AppUser user = new AppUser(tenantId, "owner@example.com", "hash", UserRole.OWNER, clock.instant());
        String token = service.generate(user);
        AuthenticatedUser parsed = service.parse(token);
        assertThat(parsed.userId()).isEqualTo(user.getId());
        assertThat(parsed.tenantId()).isEqualTo(tenantId);
        assertThat(parsed.role()).isEqualTo(UserRole.OWNER);
        assertThat(service.expiresAt(token)).isEqualTo(clock.instant().plusSeconds(3600));
    }

    @Test void rejectsTokenSignedWithAnotherSecret() {
        AppUser user = new AppUser(UUID.randomUUID(), "owner@example.com", "hash", UserRole.OWNER, clock.instant());
        String token = service.generate(user);
        JwtService other = new JwtService(new JwtProperties("another-secret-that-is-at-least-32-bytes", 60), clock);
        assertThatThrownBy(() -> other.parse(token)).isInstanceOf(JwtException.class);
    }
}
