package in.feekeeper.notification;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderMessageFormatterTest {
    private final ReminderMessageFormatter formatter = new ReminderMessageFormatter();

    @Test
    void formatsExpiringReminderFromCentralTemplate() {
        String text = formatter.format(message(ReminderVariant.EXPIRING_IN_3_DAYS));

        assertThat(text).contains("Hello Asha", "Fit Gym", "expiring in 3 days",
                "Amount due: \u20b91000.00", "19 Nov 2026", "UPI ID: fit@bank");
    }

    @Test
    void formatsOverdueReminderFromCentralTemplate() {
        String text = formatter.format(message(ReminderVariant.OVERDUE));

        assertThat(text).contains("Hello Asha", "Fit Gym", "membership fee is overdue",
                "Amount due: \u20b91000.00", "19 Nov 2026", "UPI ID: fit@bank");
    }

    private ReminderMessage message(ReminderVariant variant) {
        return new ReminderMessage(UUID.randomUUID(), UUID.randomUUID(), "+919876543210", "Asha", "Fit Gym",
                new BigDecimal("1000.00"), LocalDate.of(2026, 11, 19), "fit@bank", variant);
    }
}
