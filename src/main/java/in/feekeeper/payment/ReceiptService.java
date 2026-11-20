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
            Document document = new Document(PageSize.A5, 38, 38, 34, 32);
            PdfWriter.getInstance(document, output);
            document.open();

            document.add(header(tenant));
            document.add(new Paragraph(" ", font(5, Font.NORMAL, MUTED)));
            document.add(receiptMeta(payment));
            document.add(new Paragraph(" ", font(5, Font.NORMAL, MUTED)));
            document.add(sectionTitle("MEMBER DETAILS"));
            document.add(detailsTable(new String[][]{
                    {"Member name", member.getFullName()},
                    {"Phone", member.getPhone()}
            }));
            document.add(new Paragraph(" ", font(5, Font.NORMAL, MUTED)));
            document.add(sectionTitle("PAYMENT SUMMARY"));
            document.add(lineItems(tenant, member, payment));
            document.add(new Paragraph(" ", font(5, Font.NORMAL, MUTED)));
            document.add(amountPanel(tenant, payment));
            document.add(new Paragraph(" ", font(5, Font.NORMAL, MUTED)));
            document.add(detailsTable(new String[][]{
                    {"Payment method", payment.getPaymentMode() == PaymentMode.UPI ? "UPI" : "Cash"},
                    {"Membership renewed until", formatDate(payment.getNewExpiryDate())}
            }));
            document.add(new Paragraph(" ", font(7, Font.NORMAL, MUTED)));
            document.add(footer(tenant));
            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate receipt", ex);
        }
    }

    private PdfPTable header(Tenant tenant) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{3.1f, 1.4f});
        table.setWidthPercentage(100);
        Paragraph brandText = new Paragraph();
        brandText.add(new Chunk(tenant.getBusinessName() + "\n", font(17, Font.BOLD, Color.WHITE)));
        brandText.add(new Chunk("FEEKEEPER PAYMENT RECEIPT", font(7, Font.BOLD, new Color(202, 220, 240))));
        PdfPCell brand = new PdfPCell(brandText);
        brand.setBackgroundColor(BRAND);
        brand.setPadding(14);
        brand.setBorder(Rectangle.NO_BORDER);
        brand.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(brand);
        PdfPCell receipt = cell("PAID\n\nRECEIPT", font(10, Font.BOLD, Color.WHITE), ACCENT, 10, 7, 10, 7);
        receipt.setBorder(Rectangle.NO_BORDER);
        receipt.setHorizontalAlignment(Element.ALIGN_CENTER);
        receipt.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(receipt);

        String contact = tenant.getOwnerName() + "  |  " + tenant.getPhone();
        if (tenant.getUpiId() != null && !tenant.getUpiId().isBlank()) contact += "\nUPI: " + tenant.getUpiId();
        PdfPCell contactCell = cell(contact, font(8, Font.NORMAL, INK), PALE, 8, 10, 8, 10);
        contactCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(contactCell);
        PdfPCell paid = cell("ISSUED BY\n" + tenant.getBusinessName(), font(8, Font.BOLD, ACCENT), PALE, 8, 7, 8, 7);
        paid.setBorder(Rectangle.NO_BORDER);
        paid.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(paid);
        return table;
    }

    private PdfPTable receiptMeta(Payment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.4f, 1f});
        table.setWidthPercentage(100);
        table.addCell(metaBlock("RECEIPT NUMBER", payment.getReceiptNumber()));
        table.addCell(metaBlock("PAYMENT DATE", formatDate(payment.getPaymentDate())));
        return table;
    }

    private PdfPTable lineItems(Tenant tenant, Member member, Payment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2.2f, 1f});
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 1f});
        PdfPCell description = cell("DESCRIPTION", font(7, Font.BOLD, Color.WHITE), BRAND, 8, 9, 8, 9);
        description.setBorder(Rectangle.NO_BORDER);
        PdfPCell total = cell("AMOUNT", font(7, Font.BOLD, Color.WHITE), BRAND, 8, 9, 8, 9);
        total.setBorder(Rectangle.NO_BORDER);
        total.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(description);
        table.addCell(total);

        addLineItem(table, member.getPlanName() + " membership (" + member.getPlanDurationDays() + " days)",
                money(tenant, member.getPlanAmount()), PALE);
        if (payment.getDiscountAmount() != null && payment.getDiscountAmount().signum() > 0) {
            addLineItem(table, "Promotion discount", "- " + money(tenant, payment.getDiscountAmount()), Color.WHITE);
        }
        return table;
    }

    private void addLineItem(PdfPTable table, String description, String amount, Color background) {
        PdfPCell detail = cell(description, font(8, Font.NORMAL, INK), background, 9, 9, 9, 9);
        detail.setBorder(Rectangle.BOTTOM);
        detail.setBorderColorBottom(LINE);
        PdfPCell value = cell(amount, font(8, Font.BOLD, INK), background, 9, 9, 9, 9);
        value.setBorder(Rectangle.BOTTOM);
        value.setBorderColorBottom(LINE);
        value.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(detail);
        table.addCell(value);
    }

    private String money(Tenant tenant, java.math.BigDecimal amount) {
        return tenant.getCurrencySymbol() + " " + tenant.getCurrencyCode() + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
        PdfPCell amount = cell("TOTAL PAID\n" + money(tenant, payment.getAmountPaid()),
                font(15, Font.BOLD, Color.WHITE), BRAND, 14, 13, 14, 13);
        amount.setBorder(Rectangle.NO_BORDER);
        amount.setVerticalAlignment(Element.ALIGN_MIDDLE);
        panel.addCell(amount);
        PdfPCell status = cell("PAYMENT RECEIVED\nPAID IN FULL", font(9, Font.BOLD, Color.WHITE), ACCENT, 14, 9, 14, 9);
        status.setBorder(Rectangle.NO_BORDER);
        status.setHorizontalAlignment(Element.ALIGN_CENTER);
        status.setVerticalAlignment(Element.ALIGN_MIDDLE);
        panel.addCell(status);
        return panel;
    }


    private PdfPTable footer(Tenant tenant) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = cell("Thank you for your payment, " + tenant.getBusinessName() + ".\nThis is a computer-generated payment receipt; no signature is required.",
                font(8, Font.NORMAL, MUTED), Color.WHITE, 10, 2, 5, 2);
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
