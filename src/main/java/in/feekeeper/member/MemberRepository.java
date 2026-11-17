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

    @Query("select m.phone from Member m where m.tenantId = :tenantId and m.phone in :phones")
    List<String> findPhonesUsedByTenant(@Param("tenantId") UUID tenantId, @Param("phones") Collection<String> phones);

    default Page<Member> search(UUID tenantId, String search, MemberStatus status, Pageable pageable) {
        if (search == null && status == null) {
            return findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        }
        if (search == null) {
            return findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status, pageable);
        }
        if (status == null) {
            return searchByText(tenantId, search, pageable);
        }
        return searchByTextAndStatus(tenantId, search, status, pageable);
    }

    Page<Member> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);
    Page<Member> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, MemberStatus status,
                                                           Pageable pageable);

    @Query(value = "select m from Member m where m.tenantId = :tenantId and m.deletedAt is null " +
            "and (lower(m.fullName) like lower(concat('%', :search, '%')) or m.phone like concat('%', :search, '%'))")
    Page<Member> searchByText(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);

    @Query(value = "select m from Member m where m.tenantId = :tenantId and m.deletedAt is null " +
            "and m.status = :status and (lower(m.fullName) like lower(concat('%', :search, '%')) " +
            "or m.phone like concat('%', :search, '%'))")
    Page<Member> searchByTextAndStatus(@Param("tenantId") UUID tenantId, @Param("search") String search,
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
