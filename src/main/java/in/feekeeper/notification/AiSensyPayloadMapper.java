package in.feekeeper.notification;

import java.util.*;

final class AiSensyPayloadMapper implements ProviderPayloadMapper {
    @Override
    public ProviderRequest map(ReminderMessage message, String formattedText, WhatsAppProperties properties) {
        return new ProviderRequest("/campaign/t1/api/v2",
                Map.of("X-AiSensy-API-Key", properties.apiKey()),
                Map.of("campaignName", properties.templateName(),
                        "destination", digits(message.phone()),
                        "userName", message.memberName(),
                        "templateParams", List.of(formattedText)));
    }

    private String digits(String phone) { return phone.replaceAll("\\D", ""); }
}
