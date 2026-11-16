package in.feekeeper.tenant;

import in.feekeeper.auth.AuthenticatedUser;
import org.springframework.security.core.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class TenantContext {
    private TenantContext() { }

    public static UUID getCurrentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }
        return user.tenantId();
    }
}
