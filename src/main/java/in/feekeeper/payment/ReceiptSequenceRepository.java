package in.feekeeper.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ReceiptSequenceRepository extends JpaRepository<ReceiptSequence, ReceiptSequenceId> {
    @Modifying
    @Query(value = "insert into receipt_sequences (tenant_id, year, last_value) values (:tenantId, :year, 0) on conflict do nothing",
            nativeQuery = true)
    void initialize(@Param("tenantId") UUID tenantId, @Param("year") int year);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReceiptSequence r where r.tenantId = :tenantId and r.year = :year")
    Optional<ReceiptSequence> findForUpdate(@Param("tenantId") UUID tenantId, @Param("year") int year);
}
