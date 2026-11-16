package in.feekeeper.tenant;

import in.feekeeper.tenant.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/settings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tenant settings", description = "Current tenant business settings")
public class TenantSettingsController {
    private final TenantSettingsService service;

    public TenantSettingsController(TenantSettingsService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Read tenant settings")
    public TenantSettingsResponse get() { return service.get(); }

    @PutMapping
    @Operation(summary = "Update tenant settings")
    public TenantSettingsResponse update(@Valid @RequestBody TenantSettingsRequest request) {
        return service.update(request);
    }
}
