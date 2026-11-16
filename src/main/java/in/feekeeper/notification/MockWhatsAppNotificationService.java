package in.feekeeper.notification;

import org.slf4j.*;

import java.util.UUID;

public class MockWhatsAppNotificationService implements WhatsAppNotificationService {
    private static final Logger log = LoggerFactory.getLogger(MockWhatsAppNotificationService.class);
    private final ReminderMessageFormatter formatter;

    public MockWhatsAppNotificationService(ReminderMessageFormatter formatter) { this.formatter = formatter; }

    @Override
    public NotificationResult send(ReminderMessage message) {
        log.info("Mock WhatsApp reminder for member {}: {}", message.memberId(), formatter.format(message));
        return NotificationResult.sent("mock-" + UUID.randomUUID());
    }
}
