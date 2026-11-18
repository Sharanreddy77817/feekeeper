package in.feekeeper.tenant.dto;

import jakarta.validation.constraints.*;

public record TenantSettingsRequest(
        @NotBlank @Size(max = 150) String businessName,
        @NotBlank @Size(max = 120) String ownerName,
        @NotBlank @Pattern(regexp = "^(?:\\+91)?[6-9]\\d{9}$", message = "must be a valid Indian mobile number") String phone,
        @Size(max = 100) @Pattern(regexp = "^[A-Za-z0-9._-]+@[A-Za-z][A-Za-z0-9.-]+$", message = "must be a valid UPI ID") String upiId,
        @Pattern(regexp="^(USD|EUR|GBP|AED|INR)$") String currencyCode,
        @Size(max=8) String currencySymbol,
        @Size(max=50) String timeZone
) { public TenantSettingsRequest(String businessName,String ownerName,String phone,String upiId){this(businessName,ownerName,phone,upiId,null,null,null);} }
