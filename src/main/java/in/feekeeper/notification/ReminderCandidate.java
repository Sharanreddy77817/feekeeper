package in.feekeeper.notification;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record ReminderCandidate(
        UUID memberId,
        UUID tenantId,
        String phone,
        String memberName,
        BigDecimal planAmount,
        LocalDate expiryDate,
        LocalDate lastReminderSentDate,
        Instant deletedAt,
        String businessName,
        String upiId
) { }
