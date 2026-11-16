package in.feekeeper.auth.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 150) String businessName,
        @NotBlank @Size(max = 120) String ownerName,
        @NotBlank @Pattern(regexp = "^(?:\\+91)?[6-9]\\d{9}$", message = "must be a valid Indian mobile number") String phone,
        @Size(max = 100) String upiId,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) { }
