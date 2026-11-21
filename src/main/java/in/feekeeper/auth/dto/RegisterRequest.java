package in.feekeeper.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 150) String businessName,
        @NotBlank @Size(max = 120) String ownerName,
        @NotBlank @Pattern(regexp = "^(?:\\+91)?[6-9]\\d{9}$", message = "must be a valid Indian mobile number") String phone,
        @Size(max = 100) String upiId,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Pattern(regexp = "^(ANDROMEDA|NEBULA|MILKY_WAY)$", message = "must be ANDROMEDA, NEBULA, or MILKY_WAY") String receiptTheme,
        @NotBlank(message = "Secret registration key is required") String secretKey
) {
    public RegisterRequest(String businessName, String ownerName, String phone, String upiId, String email, String password) {
        this(businessName, ownerName, phone, upiId, email, password, "ANDROMEDA", "zovak2026");
    }

    public RegisterRequest(String businessName, String ownerName, String phone, String upiId, String email, String password, String receiptTheme) {
        this(businessName, ownerName, phone, upiId, email, password, receiptTheme, "zovak2026");
    }
}
