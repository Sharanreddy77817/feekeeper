package in.feekeeper.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Payment> findByTenantIdAndReceiptNumber(UUID tenantId, String receiptNumber);
    Optional<Payment> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    @Query("""
            select p from Payment p where p.tenantId = :tenantId
              and (:memberId is null or p.memberId = :memberId)
              and (:fromDate is null or p.paymentDate >= :fromDate)
              and (:toDate is null or p.paymentDate <= :toDate)
            """)
    Page<Payment> search(@Param("tenantId") UUID tenantId, @Param("memberId") UUID memberId,
                         @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
                         Pageable pageable);

    @Query("select coalesce(sum(p.amountPaid), 0) from Payment p where p.tenantId = :tenantId and p.paymentDate between :from and :to")
    BigDecimal revenueBetween(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
