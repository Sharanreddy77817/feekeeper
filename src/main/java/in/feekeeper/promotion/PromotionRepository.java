package in.feekeeper.promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PromotionRepository extends JpaRepository<Promotion,UUID> {
 Optional<Promotion> findByTenantIdAndCode(UUID tenantId,String code);
 List<Promotion> findByTenantIdAndActiveTrue(UUID tenantId);
}
