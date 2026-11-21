package in.feekeeper.tenant;

import in.feekeeper.common.exception.BadRequestException;
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
        String targetTheme = request.receiptTheme() == null ? tenant.getReceiptTheme() : request.receiptTheme().trim();
        if (targetTheme != null && !tenant.hasThemeAccess(targetTheme)) {
            throw new BadRequestException("THEME_LOCKED",
                    "Theme " + targetTheme + " is a premium design (+₹30). Please unlock it before selecting.");
        }
        tenant.updateSettings(request.businessName().trim(), request.ownerName().trim(),
                normalizePhone(request.phone()), normalizeUpi(request.upiId()),
                request.currencyCode()==null?tenant.getCurrencyCode():request.currencyCode(),
                request.currencySymbol()==null?symbol(request.currencyCode(),tenant.getCurrencySymbol()):request.currencySymbol(),
                validZone(request.timeZone(),tenant.getTimeZone()),
                targetTheme);
        return TenantSettingsResponse.from(tenant);
    }

    @Transactional
    public TenantSettingsResponse unlockTheme(String theme) {
        Tenant tenant = currentTenant();
        tenant.unlockTheme(theme);
        tenant.updateReceiptTheme(theme);
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
    private String symbol(String code,String fallback){if(code==null)return fallback;return switch(code){case "USD"->"$";case "EUR"->"€";case "GBP"->"£";case "AED"->"د.إ";case "INR"->"₹";default->fallback;};}
    private String validZone(String zone,String fallback){if(zone==null||zone.isBlank())return fallback;try{java.time.ZoneId.of(zone);return zone;}catch(java.time.DateTimeException ex){throw new BadRequestException("INVALID_TIME_ZONE","Invalid time zone");}}
}
