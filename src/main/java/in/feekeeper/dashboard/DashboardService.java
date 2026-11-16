package in.feekeeper.dashboard;

import in.feekeeper.dashboard.dto.DashboardResponse;
import in.feekeeper.member.MemberRepository;
import in.feekeeper.member.MemberStatus;
import in.feekeeper.payment.PaymentRepository;
import in.feekeeper.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class DashboardService {
    private final MemberRepository members;
    private final PaymentRepository payments;
    private final Clock clock;

    public DashboardService(MemberRepository members, PaymentRepository payments, Clock clock) {
        this.members = members;
        this.payments = payments;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        LocalDate today = LocalDate.now(clock);
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate lastOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        return new DashboardResponse(
                members.countByTenantAndStatus(tenantId, MemberStatus.ACTIVE),
                members.countExpiringBetween(tenantId, today, today.plusDays(5)),
                members.countOverdue(tenantId, today),
                payments.revenueBetween(tenantId, firstOfMonth, lastOfMonth));
    }
}
