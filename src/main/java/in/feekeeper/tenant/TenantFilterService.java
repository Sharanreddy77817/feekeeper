package in.feekeeper.tenant;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantFilterService {
    private final EntityManager entityManager;
    public TenantFilterService(EntityManager entityManager) { this.entityManager = entityManager; }

    public void enableFor(UUID tenantId) {
        entityManager.unwrap(Session.class).enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }

    public void enableForCurrentTenant() { enableFor(TenantContext.getCurrentTenantId()); }
}
