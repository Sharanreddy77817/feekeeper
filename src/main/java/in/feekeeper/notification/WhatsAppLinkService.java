package in.feekeeper.notification;

import in.feekeeper.common.exception.NotFoundException;
import in.feekeeper.member.*;
import in.feekeeper.member.dto.WhatsAppLinkResponse;
import in.feekeeper.tenant.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.UUID;

@Service
public class WhatsAppLinkService {
    private final MemberRepository members;
    private final TenantRepository tenants;
    private final ReminderMessageFormatter formatter;
    private final Clock clock;

    public WhatsAppLinkService(MemberRepository members, TenantRepository tenants,
                              ReminderMessageFormatter formatter, Clock clock) {
        this.members = members;
        this.tenants = tenants;
        this.formatter = formatter;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WhatsAppLinkResponse create(UUID memberId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Member member = members.findByIdAndTenantIdAndDeletedAtIsNull(memberId, tenantId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant not found"));
        ReminderVariant variant = member.getExpiryDate().isBefore(LocalDate.now(clock))
                ? ReminderVariant.OVERDUE : ReminderVariant.EXPIRING_IN_3_DAYS;
        ReminderMessage message = new ReminderMessage(member.getId(), tenantId, member.getPhone(), member.getFullName(),
                tenant.getBusinessName(), member.getPlanAmount(), member.getExpiryDate(), tenant.getUpiId(), variant);
        String phone = normalizeForWaMe(member.getPhone());
        String url = UriComponentsBuilder.fromUriString("https://wa.me/" + phone)
                .queryParam("text", formatter.format(message)).build().encode().toUriString();
        return new WhatsAppLinkResponse(url);
    }

    private String normalizeForWaMe(String phone) {
        String digits = phone.replaceAll("\\D", "");
        return digits.length() == 10 ? "91" + digits : digits;
    }
}
