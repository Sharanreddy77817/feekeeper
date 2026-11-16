package in.feekeeper.member;

import in.feekeeper.common.exception.*;
import in.feekeeper.member.dto.*;
import in.feekeeper.tenant.TenantContext;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Service
public class MemberService {
    private final MemberRepository repository;
    private final MemberStatusCalculator statusCalculator;
    private final Clock clock;

    public MemberService(MemberRepository repository, MemberStatusCalculator statusCalculator, Clock clock) {
        this.repository = repository; this.statusCalculator = statusCalculator; this.clock = clock;
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        String phone = normalizePhone(request.phone());
        ensurePhoneAvailable(tenantId, phone, null);
        var expiry = request.joinDate().plusDays(request.planDurationDays());
        Member member = new Member(tenantId, request.fullName().trim(), phone, request.planName().trim(),
                request.planAmount(), request.planDurationDays(), request.joinDate(), expiry,
                statusCalculator.calculate(expiry), clock.instant());
        return MemberResponse.from(repository.save(member));
    }

    @Transactional(readOnly = true)
    public MemberResponse get(UUID id) { return MemberResponse.from(findActive(id)); }

    @Transactional
    public MemberResponse update(UUID id, MemberRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Member member = repository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        String phone = normalizePhone(request.phone());
        ensurePhoneAvailable(tenantId, phone, id);
        var expiry = request.joinDate().plusDays(request.planDurationDays());
        member.update(request.fullName().trim(), phone, request.planName().trim(), request.planAmount(),
                request.planDurationDays(), request.joinDate(), expiry, statusCalculator.calculate(expiry), clock.instant());
        return MemberResponse.from(member);
    }

    @Transactional
    public void delete(UUID id) { findActive(id).softDelete(clock.instant()); }

    @Transactional(readOnly = true)
    public Page<MemberResponse> list(String search, MemberStatus status, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return repository.search(TenantContext.getCurrentTenantId(), normalizedSearch, status, pageable)
                .map(MemberResponse::from);
    }

    private Member findActive(UUID id) {
        return repository.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("Member not found"));
    }

    private void ensurePhoneAvailable(UUID tenantId, String phone, UUID memberId) {
        boolean exists = memberId == null ? repository.existsByTenantIdAndPhone(tenantId, phone)
                : repository.existsByTenantIdAndPhoneAndIdNot(tenantId, phone, memberId);
        if (exists) throw new ConflictException("MEMBER_PHONE_EXISTS", "A member with this phone already exists");
    }

    private String normalizePhone(String phone) { return phone.startsWith("+91") ? phone : "+91" + phone; }
}
