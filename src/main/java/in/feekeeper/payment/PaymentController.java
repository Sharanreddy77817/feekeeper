package in.feekeeper.payment;

import in.feekeeper.payment.dto.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Tenant-scoped payments and receipts")
public class PaymentController {
    private final PaymentService service;
    private final ReceiptService receipts;
    public PaymentController(PaymentService service, ReceiptService receipts) { this.service = service; this.receipts = receipts; }

    @PostMapping("/mark-paid/{memberId}")
    @Operation(summary = "Record a payment and renew a member")
    public PaymentResponse markPaid(@PathVariable UUID memberId,
                                    @RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @Valid @RequestBody MarkPaidRequest request) {
        return service.markPaid(memberId, request, idempotencyKey);
    }

    @GetMapping
    @Operation(summary = "List and filter payments")
    public Page<PaymentResponse> list(@RequestParam(required = false) UUID memberId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                      @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(memberId, fromDate, toDate, pageable);
    }

    @GetMapping(value = "/{id}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download a PDF receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable UUID id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt-" + id + ".pdf")
                .body(receipts.createPdf(id));
    }
}
