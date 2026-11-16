package in.feekeeper.notification;

import java.util.*;

final class InteraktPayloadMapper implements ProviderPayloadMapper {
    @Override
    public ProviderRequest map(ReminderMessage message, String formattedText, WhatsAppProperties properties) {
        String digits = message.phone().replaceAll("\\D", "");
        String nationalNumber = digits.startsWith("91") && digits.length() == 12 ? digits.substring(2) : digits;
        Map<String, Object> template = Map.of(
                "name", properties.templateName(),
                "languageCode", properties.languageCode(),
                "bodyValues", List.of(formattedText));
        return new ProviderRequest("/v1/public/message/",
                Map.of("Authorization", "Basic " + properties.apiKey()),
                Map.of("countryCode", "+91", "phoneNumber", nationalNumber,
                        "type", "Template", "template", template));
    }
}
