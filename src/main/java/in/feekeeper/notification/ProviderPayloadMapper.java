package in.feekeeper.notification;

import java.util.Map;

interface ProviderPayloadMapper {
    ProviderRequest map(ReminderMessage message, String formattedText, WhatsAppProperties properties);

    record ProviderRequest(String path, Map<String, String> headers, Map<String, Object> body) { }
}
