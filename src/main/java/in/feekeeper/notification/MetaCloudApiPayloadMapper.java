package in.feekeeper.notification;

import java.util.List;
import java.util.Map;

final class MetaCloudApiPayloadMapper implements ProviderPayloadMapper {
    private final ReminderMessageFormatter formatter;

    MetaCloudApiPayloadMapper(ReminderMessageFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public ProviderRequest map(ReminderMessage message, String formattedText, WhatsAppProperties properties) {
        String digits = message.phone().replaceAll("\\D", "");
        if (digits.length() == 10) digits = "91" + digits;
        List<Map<String, String>> parameters = formatter.templateParameters(message).stream()
                .map(value -> Map.of("type", "text", "text", value))
                .toList();
        Map<String, Object> bodyComponent = Map.of("type", "body", "parameters", parameters);
        Map<String, Object> template = Map.of(
                "name", properties.templateName(),
                "language", Map.of("code", properties.languageCode()),
                "components", List.of(bodyComponent));
        return new ProviderRequest("/%s/messages".formatted(properties.phoneNumberId()),
                Map.of("Authorization", "Bearer " + properties.apiKey()),
                Map.of("messaging_product", "whatsapp", "recipient_type", "individual",
                        "to", digits, "type", "template", "template", template));
    }
}
