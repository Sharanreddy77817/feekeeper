package in.feekeeper.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Payment> findAllByTenantIdAndMemberId(UUID tenantId, UUID memberId);
    Optional<Payment> findByTenantIdAndReceiptNumber(UUID tenantId, String receiptNumber);
}
