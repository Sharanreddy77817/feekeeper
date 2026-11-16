package in.feekeeper.notification;

public interface WhatsAppNotificationService {
    NotificationResult send(ReminderMessage message);
}
