package in.feekeeper.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
    Optional<Member> findByTenantIdAndPhoneAndDeletedAtIsNull(UUID tenantId, String phone);
    List<Member> findAllByTenantIdAndDeletedAtIsNull(UUID tenantId);
    List<Member> findAllByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, MemberStatus status);
}
