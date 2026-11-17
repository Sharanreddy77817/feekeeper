package in.feekeeper.member;

import in.feekeeper.member.dto.*;
import in.feekeeper.notification.WhatsAppLinkService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/members")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Members", description = "Tenant-scoped member management")
public class MemberController {
    private final MemberService service;
    private final MemberCsvImportService csvImportService;
    private final WhatsAppLinkService whatsAppLinks;
    public MemberController(MemberService service, MemberCsvImportService csvImportService, WhatsAppLinkService whatsAppLinks) {
        this.service = service;
        this.csvImportService = csvImportService;
        this.whatsAppLinks = whatsAppLinks;
    }

    @PostMapping
    @Operation(summary = "Create a member")
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import members from CSV", description = "Imports up to 500 member rows. Required columns: fullName, phone, planName, planAmount, planDurationDays, joinDate. Invalid rows are returned with row-level errors.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(type = "string", format = "binary"))))
    public MemberCsvImportResponse importCsv(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) throw new in.feekeeper.common.exception.BadRequestException("EMPTY_CSV", "Choose a non-empty CSV file");
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            throw new in.feekeeper.common.exception.BadRequestException("INVALID_CSV_FILE", "Choose a .csv file");
        }
        try {
            return csvImportService.importCsv(file.getBytes());
        } catch (java.io.IOException ex) {
            throw new in.feekeeper.common.exception.BadRequestException("INVALID_CSV_FILE", "The CSV file could not be read");
        }
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
