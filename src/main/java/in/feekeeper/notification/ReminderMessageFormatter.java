package in.feekeeper.notification;

import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class ReminderMessageFormatter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public String format(ReminderMessage message) {
        List<String> values = templateParameters(message);
        return "Hello %s,%n%nThis is a friendly reminder from %s. %s Amount due: ₹%s. Expiry date: %s.%nUPI ID: %s.%n%nThank you."
                .formatted(values.toArray());
    }

    /** Values map in order to {{1}} through {{6}} in the approved Meta template body. */
    public List<String> templateParameters(ReminderMessage message) {
        String statusText = switch (message.variant()) {
            case EXPIRING_IN_3_DAYS -> "Your membership is expiring in 3 days.";
            case OVERDUE -> "Your membership fee is overdue.";
        };
        String upi = message.upiId() == null || message.upiId().isBlank()
                ? "not available; please contact the business to pay"
                : message.upiId();
        return List.of(message.memberName(), message.businessName(), statusText,
                message.dueAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                DATE.format(message.expiryDate()), upi);
    }

    // A future Hindi formatter can implement the same ReminderMessage contract.
}
