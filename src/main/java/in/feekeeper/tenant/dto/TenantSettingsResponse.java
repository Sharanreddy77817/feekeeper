package in.feekeeper.tenant.dto;

import in.feekeeper.tenant.Tenant;

public record TenantSettingsResponse(String businessName, String ownerName, String phone, String upiId, String currencyCode, String currencySymbol, String timeZone) {
    public static TenantSettingsResponse from(Tenant tenant) {
        return new TenantSettingsResponse(tenant.getBusinessName(), tenant.getOwnerName(), tenant.getPhone(), tenant.getUpiId(), tenant.getCurrencyCode(), tenant.getCurrencySymbol(), tenant.getTimeZone());
    }
}
