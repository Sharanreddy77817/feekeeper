package in.feekeeper.dashboard;

import in.feekeeper.dashboard.dto.DashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Tenant-scoped business totals")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get dashboard totals")
    public DashboardResponse get() { return service.get(); }
}
