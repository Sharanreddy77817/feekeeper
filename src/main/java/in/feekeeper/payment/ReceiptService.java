package in.feekeeper.payment;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import in.feekeeper.common.exception.NotFoundException;
import in.feekeeper.member.*;
import in.feekeeper.tenant.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
public class ReceiptService {
    private final PaymentRepository payments;
    private final MemberRepository members;
    private final TenantRepository tenants;

    public ReceiptService(PaymentRepository payments, MemberRepository members, TenantRepository tenants) {
        this.payments = payments; this.members = members; this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public byte[] createPdf(UUID paymentId) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Payment payment = payments.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        Member member = members.findByIdAndTenantId(payment.getMemberId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Member not found"));
        Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant not found"));
        return render(tenant, member, payment);
    }

    private byte[] render(Tenant tenant, Member member, Payment payment) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A5, 36, 36, 36, 36);
            PdfWriter.getInstance(document, output);
            document.open();
            Font title = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font heading = new Font(Font.HELVETICA, 11, Font.BOLD);
            document.add(new Paragraph(tenant.getBusinessName(), title));
            document.add(new Paragraph("Payment Receipt", heading));
            document.add(Chunk.NEWLINE);
            addLine(document, "Receipt number", payment.getReceiptNumber());
            addLine(document, "Member", member.getFullName());
            addLine(document, "Plan", member.getPlanName());
            addLine(document, "Amount", "INR " + payment.getAmountPaid().toPlainString());
            addLine(document, "Payment mode", payment.getPaymentMode().name());
            addLine(document, "Payment date", payment.getPaymentDate().toString());
            addLine(document, "New expiry date", payment.getNewExpiryDate().toString());
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Thank you for your payment."));
            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate receipt", ex);
        }
    }

    private void addLine(Document document, String label, String value) throws DocumentException {
        Paragraph line = new Paragraph();
        line.add(new Chunk(label + ": ", new Font(Font.HELVETICA, 10, Font.BOLD)));
        line.add(new Chunk(value, new Font(Font.HELVETICA, 10)));
        document.add(line);
    }
}
