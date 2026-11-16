package in.feekeeper.member;

import in.feekeeper.member.dto.*;
import in.feekeeper.notification.WhatsAppLinkService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Members", description = "Tenant-scoped member management")
public class MemberController {
    private final MemberService service;
    private final WhatsAppLinkService whatsAppLinks;
    public MemberController(MemberService service, WhatsAppLinkService whatsAppLinks) {
        this.service = service;
        this.whatsAppLinks = whatsAppLinks;
    }

    @PostMapping
    @Operation(summary = "Create a member")
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a member")
    public MemberResponse get(@PathVariable UUID id) { return service.get(id); }

    @PutMapping("/{id}")
    @Operation(summary = "Update a member")
    public MemberResponse update(@PathVariable UUID id, @Valid @RequestBody MemberRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a member")
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @GetMapping
    @Operation(summary = "List, search, and filter members")
    public Page<MemberResponse> list(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) MemberStatus status,
                                     @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return service.list(search, status, pageable);
    }

    @GetMapping("/{id}/whatsapp-link")
    @Operation(summary = "Create a prefilled wa.me reminder link")
    public WhatsAppLinkResponse whatsAppLink(@PathVariable UUID id) { return whatsAppLinks.create(id); }
}
