package in.feekeeper.notification;

public record NotificationResult(boolean success, String providerMessageId, String error) {
    public static NotificationResult sent(String providerMessageId) {
        return new NotificationResult(true, providerMessageId, null);
    }

    public static NotificationResult failed(String error) {
        return new NotificationResult(false, null, error);
    }
}
