package in.feekeeper.tenant;

import in.feekeeper.common.exception.NotFoundException;
import in.feekeeper.tenant.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSettingsService {
    private final TenantRepository tenants;

    public TenantSettingsService(TenantRepository tenants) { this.tenants = tenants; }

    @Transactional(readOnly = true)
    public TenantSettingsResponse get() { return TenantSettingsResponse.from(currentTenant()); }

    @Transactional
    public TenantSettingsResponse update(TenantSettingsRequest request) {
        Tenant tenant = currentTenant();
        tenant.updateSettings(request.businessName().trim(), request.ownerName().trim(),
                normalizePhone(request.phone()), normalizeUpi(request.upiId()));
        return TenantSettingsResponse.from(tenant);
    }

    private Tenant currentTenant() {
        return tenants.findById(TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
    }

    private String normalizePhone(String phone) {
        String digits = phone.trim();
        return digits.startsWith("+91") ? digits : "+91" + digits;
    }

    private String normalizeUpi(String upiId) {
        return upiId == null || upiId.isBlank() ? null : upiId.trim().toLowerCase();
    }
}
