package in.feekeeper.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByEmailIgnoreCase(String email);
}
