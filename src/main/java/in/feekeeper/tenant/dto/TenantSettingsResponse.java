package in.feekeeper.tenant.dto;

import in.feekeeper.tenant.Tenant;

public record TenantSettingsResponse(String businessName, String ownerName, String phone, String upiId) {
    public static TenantSettingsResponse from(Tenant tenant) {
        return new TenantSettingsResponse(tenant.getBusinessName(), tenant.getOwnerName(), tenant.getPhone(), tenant.getUpiId());
    }
}
