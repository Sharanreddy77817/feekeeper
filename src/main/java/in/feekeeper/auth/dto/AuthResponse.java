package in.feekeeper.auth.dto;

import in.feekeeper.auth.UserRole;
import java.time.Instant;
import java.util.UUID;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt,
                           UUID userId, UUID tenantId, UserRole role) { }
