package in.feekeeper.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ReceiptSequenceRepository extends JpaRepository<ReceiptSequence, ReceiptSequenceId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReceiptSequence r where r.tenantId = :tenantId and r.year = :year")
    Optional<ReceiptSequence> findForUpdate(@Param("tenantId") UUID tenantId, @Param("year") int year);
}
