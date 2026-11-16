package in.feekeeper.auth;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {
    @Id private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(nullable = false, unique = true, length = 254) private String email;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserRole role;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public AppUser(UUID tenantId, String email, String passwordHash, UserRole role, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }
}
