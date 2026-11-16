package in.feekeeper.notification;

import org.slf4j.*;
import org.springframework.stereotype.Service;
import in.feekeeper.member.*;

import java.time.*;
import java.util.*;

@Service
public class DailyReminderService {
    private static final Logger log = LoggerFactory.getLogger(DailyReminderService.class);
    private final SystemMemberRepository members;
    private final WhatsAppNotificationService notifications;
    private final WhatsAppProperties properties;
    private final MemberStatusCalculator statusCalculator;
    private final Clock clock;

    public DailyReminderService(SystemMemberRepository members, WhatsAppNotificationService notifications,
                                WhatsAppProperties properties, MemberStatusCalculator statusCalculator, Clock clock) {
        this.members = members;
        this.notifications = notifications;
        this.properties = properties;
        this.statusCalculator = statusCalculator;
        this.clock = clock;
    }

    public void run() {
        LocalDate today = LocalDate.now(clock);
        updateStatuses();
        UUID afterId = null;
        int pageSize = Math.max(1, properties.batchSize());
        while (true) {
            List<ReminderCandidate> page = members.findPageAfter(afterId, pageSize);
            if (page.isEmpty()) return;
            for (ReminderCandidate candidate : page) process(candidate, today);
            afterId = page.getLast().memberId();
            if (page.size() < pageSize) return;
        }
    }

    private void updateStatuses() {
        UUID afterId = null;
        int pageSize = Math.max(1, properties.batchSize());
        while (true) {
            List<MemberStatusCandidate> page = members.findStatusPageAfter(afterId, pageSize);
            if (page.isEmpty()) return;
            for (MemberStatusCandidate candidate : page) {
                MemberStatus calculated = statusCalculator.calculate(candidate.expiryDate());
                if (calculated != candidate.currentStatus())
                    members.updateStatus(candidate.memberId(), calculated, clock.instant());
            }
            afterId = page.getLast().memberId();
            if (page.size() < pageSize) return;
        }
    }

    private void process(ReminderCandidate candidate, LocalDate today) {
        Optional<ReminderVariant> variant = eligibleVariant(candidate, today);
        if (variant.isEmpty()) return;
        ReminderMessage message = new ReminderMessage(candidate.memberId(), candidate.tenantId(), candidate.phone(),
                candidate.memberName(), candidate.businessName(), candidate.planAmount(), candidate.expiryDate(),
                candidate.upiId(), variant.get());
        try {
            NotificationResult result = notifications.send(message);
            if (result.success()) {
                members.markReminderSent(candidate.memberId(), candidate.tenantId(), today, clock.instant());
            } else {
                log.warn("WhatsApp reminder failed for member {} phone {}: {}", candidate.memberId(),
                        mask(candidate.phone()), result.error());
            }
        } catch (RuntimeException ex) {
            log.warn("WhatsApp reminder failed for member {} phone {} ({})", candidate.memberId(),
                    mask(candidate.phone()), ex.getClass().getSimpleName());
        }
    }

    private Optional<ReminderVariant> eligibleVariant(ReminderCandidate candidate, LocalDate today) {
        if (candidate.deletedAt() != null || today.equals(candidate.lastReminderSentDate())) return Optional.empty();
        if (candidate.expiryDate().equals(today.plusDays(3))) return Optional.of(ReminderVariant.EXPIRING_IN_3_DAYS);
        LocalDate earliestOverdue = today.minusDays(7);
        if (candidate.expiryDate().isBefore(today) && !candidate.expiryDate().isBefore(earliestOverdue))
            return Optional.of(ReminderVariant.OVERDUE);
        return Optional.empty();
    }

    private String mask(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        return digits.length() < 4 ? "****" : "********" + digits.substring(digits.length() - 4);
    }
}
