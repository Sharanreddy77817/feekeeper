package in.feekeeper.notification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReminderMessage(
        UUID memberId,
        UUID tenantId,
        String phone,
        String memberName,
        String businessName,
        BigDecimal dueAmount,
        LocalDate expiryDate,
        String upiId,
        ReminderVariant variant
) { }
