package in.feekeeper.notification;

import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class ReminderMessageFormatter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public String format(ReminderMessage message) {
        String statusText = switch (message.variant()) {
            case EXPIRING_IN_3_DAYS -> "Your membership is expiring in 3 days.";
            case OVERDUE -> "Your membership fee is overdue.";
        };
        String upi = message.upiId() == null || message.upiId().isBlank()
                ? "UPI ID: not available; please contact the business to pay."
                : "UPI ID: " + message.upiId() + ".";
        return "Hello %s,%n%nThis is a friendly reminder from %s. %s Amount due: ₹%s. Expiry date: %s.%n%s%n%nThank you."
                .formatted(message.memberName(), message.businessName(), statusText,
                        message.dueAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        DATE.format(message.expiryDate()), upi);
    }

    // A future Hindi formatter can implement the same ReminderMessage contract.
}
