package in.feekeeper.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findById(UUID tenantId);
}
