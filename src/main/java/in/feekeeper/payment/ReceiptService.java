package in.feekeeper.payment;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import in.feekeeper.common.exception.NotFoundException;
import in.feekeeper.member.*;
import in.feekeeper.tenant.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReceiptService {
    private static final Color INK = new Color(25, 42, 66);
    private static final Color MUTED = new Color(101, 116, 139);
    private static final Color BRAND = new Color(30, 64, 112);
    private static final Color ACCENT = new Color(20, 133, 111);
    private static final Color PALE = new Color(241, 246, 250);
    private static final Color LINE = new Color(222, 230, 237);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final PaymentRepository payments;
    private final MemberRepository members;
    private final TenantRepository tenants;

    public ReceiptService(PaymentRepository payments, MemberRepository members, TenantRepository tenants) {
        this.payments = payments;
        this.members = members;
        this.tenants = tenants;
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
            Document document = new Document(PageSize.A5, 34, 34, 30, 30);
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(header(tenant));
            document.add(Chunk.NEWLINE);
            document.add(receiptMeta(payment));
            document.add(Chunk.NEWLINE);
            document.add(sectionTitle("MEMBER DETAILS"));
            document.add(detailsTable(new String[][]{
                    {"Member name", member.getFullName()},
                    {"Phone", member.getPhone()},
                    {"Membership plan", member.getPlanName()},
                    {"Plan duration", member.getPlanDurationDays() + " days"}
            }));
            document.add(Chunk.NEWLINE);
            document.add(sectionTitle("PAYMENT DETAILS"));
            document.add(detailsTable(new String[][]{
                    {"Payment date", formatDate(payment.getPaymentDate())},
                    {"Payment method", payment.getPaymentMode() == PaymentMode.UPI ? "UPI" : "Cash"},
                    {"Membership valid until", formatDate(payment.getNewExpiryDate())}
            }));
            document.add(Chunk.NEWLINE);
            document.add(amountPanel(tenant, payment));
            document.add(Chunk.NEWLINE);
            document.add(footer(tenant));
            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate receipt", ex);
        }
    }

    private PdfPTable header(Tenant tenant) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{3.4f, 1.2f});
        table.setWidthPercentage(100);
        PdfPCell brand = cell(tenant.getBusinessName(), font(19, Font.BOLD, Color.WHITE), BRAND, 13, 14, 12, 14);
        brand.setBorder(Rectangle.NO_BORDER);
        brand.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(brand);
        PdfPCell receipt = cell("FEE\nRECEIPT", font(9, Font.BOLD, Color.WHITE), ACCENT, 10, 7, 10, 7);
        receipt.setBorder(Rectangle.NO_BORDER);
        receipt.setHorizontalAlignment(Element.ALIGN_CENTER);
        receipt.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(receipt);

        String contact = "Owner: " + tenant.getOwnerName() + "\nPhone: " + tenant.getPhone();
        if (tenant.getUpiId() != null && !tenant.getUpiId().isBlank()) contact += "\nUPI: " + tenant.getUpiId();
        PdfPCell contactCell = cell(contact, font(8, Font.NORMAL, INK), PALE, 8, 10, 8, 10);
        contactCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contactCell);
        PdfPCell paid = cell("PAYMENT\nRECEIVED", font(8, Font.BOLD, ACCENT), PALE, 8, 7, 8, 7);
        paid.setBorder(Rectangle.NO_BORDER);
        paid.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(paid);
        return table;
    }

    private PdfPTable receiptMeta(Payment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.4f, 1f});
        table.setWidthPercentage(100);
        table.addCell(metaBlock("RECEIPT NUMBER", payment.getReceiptNumber()));
        table.addCell(metaBlock("ISSUED ON", formatDate(payment.getPaymentDate())));
        return table;
    }

    private PdfPCell metaBlock(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(LINE);
        cell.setPaddingBottom(9);
        cell.addElement(new Paragraph(label, font(7, Font.BOLD, MUTED)));
        cell.addElement(new Paragraph(value, font(10, Font.BOLD, INK)));
        return cell;
    }

    private Paragraph sectionTitle(String title) {
        Paragraph paragraph = new Paragraph(title, font(8, Font.BOLD, BRAND));
        paragraph.setSpacingAfter(5);
        return paragraph;
    }

    private PdfPTable detailsTable(String[][] rows) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1f, 1.65f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(2);
        for (int i = 0; i < rows.length; i++) {
            Color background = i % 2 == 0 ? PALE : Color.WHITE;
            PdfPCell label = cell(rows[i][0], font(8, Font.NORMAL, MUTED), background, 7, 8, 7, 8);
            label.setBorder(Rectangle.NO_BORDER);
            PdfPCell value = cell(rows[i][1], font(9, Font.BOLD, INK), background, 7, 8, 7, 8);
            value.setBorder(Rectangle.NO_BORDER);
            table.addCell(label);
            table.addCell(value);
        }
        return table;
    }

    private PdfPTable amountPanel(Tenant tenant, Payment payment) throws DocumentException {
        PdfPTable panel = new PdfPTable(new float[]{1f, 1f});
        panel.setWidthPercentage(100);
        PdfPCell amount = cell("AMOUNT PAID\n" + tenant.getCurrencySymbol() + " " + tenant.getCurrencyCode() + " " + payment.getAmountPaid().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                font(14, Font.BOLD, Color.WHITE), BRAND, 12, 12, 12, 12);
        amount.setBorder(Rectangle.NO_BORDER);
        amount.setVerticalAlignment(Element.ALIGN_MIDDLE);
        panel.addCell(amount);
        PdfPCell status = cell("STATUS\nPAID", font(12, Font.BOLD, Color.WHITE), ACCENT, 12, 12, 12, 12);
        status.setBorder(Rectangle.NO_BORDER);
        status.setHorizontalAlignment(Element.ALIGN_CENTER);
        status.setVerticalAlignment(Element.ALIGN_MIDDLE);
        panel.addCell(status);
        return panel;
    }


    private PdfPTable footer(Tenant tenant) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = cell("Thank you for choosing " + tenant.getBusinessName() + ".\nThis is a computer-generated payment receipt.",
                font(8, Font.NORMAL, MUTED), Color.WHITE, 8, 2, 4, 2);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColorTop(LINE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        return table;
    }

    private PdfPCell cell(String text, Font font, Color background, float top, float left, float bottom, float right) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPaddingTop(top);
        cell.setPaddingLeft(left);
        cell.setPaddingBottom(bottom);
        cell.setPaddingRight(right);
        return cell;
    }

    private Font font(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private String formatDate(LocalDate date) {
        return DATE.format(date);
    }

}
