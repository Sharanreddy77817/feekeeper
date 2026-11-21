package in.feekeeper.tenant.dto;

import in.feekeeper.tenant.Tenant;

public record TenantSettingsResponse(
        String businessName,
        String ownerName,
        String phone,
        String upiId,
        String currencyCode,
        String currencySymbol,
        String timeZone,
        String receiptTheme,
        String unlockedThemes
) {
    public TenantSettingsResponse(String businessName, String ownerName, String phone, String upiId,
                                  String currencyCode, String currencySymbol, String timeZone, String receiptTheme) {
        this(businessName, ownerName, phone, upiId, currencyCode, currencySymbol, timeZone, receiptTheme, "ANDROMEDA");
    }

    public static TenantSettingsResponse from(Tenant tenant) {
        return new TenantSettingsResponse(
                tenant.getBusinessName(),
                tenant.getOwnerName(),
                tenant.getPhone(),
                tenant.getUpiId(),
                tenant.getCurrencyCode(),
                tenant.getCurrencySymbol(),
                tenant.getTimeZone(),
                tenant.getReceiptTheme(),
                tenant.getUnlockedThemes()
        );
    }
}
