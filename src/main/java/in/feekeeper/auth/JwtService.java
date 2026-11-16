package in.feekeeper.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(AppUser user) {
        Instant issuedAt = clock.instant();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("tenantId", user.getTenantId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(Duration.ofMinutes(properties.expiryMinutes()))))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = parser().parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(
                UUID.fromString(claims.get("userId", String.class)),
                UUID.fromString(claims.get("tenantId", String.class)),
                claims.getSubject(), UserRole.valueOf(claims.get("role", String.class)));
    }

    public Instant expiresAt(String token) {
        return parser().parseSignedClaims(token).getPayload().getExpiration().toInstant();
    }

    private JwtParser parser() {
        return Jwts.parser().clock(() -> Date.from(clock.instant())).verifyWith(key).build();
    }
}
