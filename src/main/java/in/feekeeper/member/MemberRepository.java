package in.feekeeper.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    Optional<Member> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Member> findByTenantIdAndPhoneAndDeletedAtIsNull(UUID tenantId, String phone);
    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);
    boolean existsByTenantIdAndPhoneAndIdNot(UUID tenantId, String phone, UUID id);

    @Query("""
            select m from Member m
            where m.tenantId = :tenantId and m.deletedAt is null
              and (:search is null or lower(m.fullName) like lower(concat('%', :search, '%'))
                   or m.phone like concat('%', :search, '%'))
              and (:status is null or m.status = :status)
            """)
    Page<Member> search(@Param("tenantId") UUID tenantId, @Param("search") String search,
                        @Param("status") MemberStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id and m.tenantId = :tenantId and m.deletedAt is null")
    Optional<Member> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("select count(m) from Member m where m.tenantId = :tenantId and m.deletedAt is null and m.status = :status")
    long countByTenantAndStatus(@Param("tenantId") UUID tenantId, @Param("status") MemberStatus status);

    @Query("select count(m) from Member m where m.tenantId = :tenantId and m.deletedAt is null and m.expiryDate between :from and :to")
    long countExpiringBetween(@Param("tenantId") UUID tenantId, @Param("from") java.time.LocalDate from,
                              @Param("to") java.time.LocalDate to);

    @Query("select count(m) from Member m where m.tenantId = :tenantId and m.deletedAt is null and m.expiryDate < :today")
    long countOverdue(@Param("tenantId") UUID tenantId, @Param("today") java.time.LocalDate today);
}
