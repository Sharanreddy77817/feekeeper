package in.feekeeper.auth;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UUID tenantId, String email, UserRole role) { }
