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
    // Professional color palette inspired by standard gym & tutorial institutions
    private static final Color BRAND_NAVY = new Color(15, 23, 42);       // #0F172A
    private static final Color BRAND_BLUE = new Color(30, 58, 138);       // #1E3A8A
    private static final Color EMERALD_DARK = new Color(4, 120, 87);      // #047857
    private static final Color EMERALD_BG = new Color(236, 253, 245);     // #ECFDF5
    private static final Color EMERALD_BORDER = new Color(110, 231, 183); // #6EE7B7
    private static final Color TEXT_MAIN = new Color(15, 23, 42);         // #0F172A
    private static final Color TEXT_MUTED = new Color(100, 116, 139);     // #64748B
    private static final Color BG_PANEL = new Color(248, 250, 252);       // #F8FAFC
    private static final Color BG_HEADER = new Color(241, 245, 249);      // #F1F5F9
    private static final Color BORDER_LINE = new Color(226, 232, 240);    // #E2E8F0

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
            // A5 dimensions: 419.53pt x 595.28pt. Margins: 22 left, 22 right, 18 top, 18 bottom
            Document document = new Document(PageSize.A5, 22, 22, 18, 18);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new ReceiptPageFrame());
            document.open();

            // 1. Header (Institute / Academy Branding + Official Paid Stamp)
            document.add(header(tenant));
            document.add(spacer(3));

            // 2. Metadata Ribbon (Receipt No, Date, Mode, Status)
            document.add(receiptMeta(payment));
            document.add(spacer(4));

            // 3. Member & Schedule Two-Column Cards
            document.add(memberAndScheduleCards(member, payment));
            document.add(spacer(4));

            // 4. Fee Particulars Itemized Table
            document.add(lineItems(tenant, member, payment));
            document.add(spacer(3));

            // 5. Payment Summary & Net Total Block
            document.add(amountPanel(tenant, member, payment));
            document.add(spacer(4));

            // 6. Important Gym & Coaching Regulations
            document.add(termsAndRegulations(payment));
            document.add(spacer(3));

            // 7. Security Digital Verification & Signatory Stamp
            document.add(signatoryBlock(tenant));
            document.add(spacer(2));

            // 8. Micro-print Bottom Footer
            document.add(footer(tenant));

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate receipt", ex);
        }
    }

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ", font(height, Font.NORMAL, TEXT_MUTED));
        p.setLeading(height);
        return p;
    }

    private PdfPTable header(Tenant tenant) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{2.7f, 1.3f});
        table.setWidthPercentage(100);

        // Left Cell: Institute / Academy Identity & Contacts
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(4);

        Paragraph orgName = new Paragraph(tenant.getBusinessName(), font(14, Font.BOLD, BRAND_NAVY));
        orgName.setLeading(16);
        left.addElement(orgName);

        Paragraph subHeading = new Paragraph("GYM & FITNESS CLUB · ACADEMIC COACHING & TUTORIALS", font(6.5f, Font.BOLD, BRAND_BLUE));
        subHeading.setLeading(9);
        subHeading.setSpacingBefore(1);
        left.addElement(subHeading);

        String contact = "Desk Helpline: " + tenant.getPhone() + "  |  Center Head: " + tenant.getOwnerName();
        if (tenant.getUpiId() != null && !tenant.getUpiId().isBlank()) {
            contact += "\nUPI ID: " + tenant.getUpiId();
        }
        Paragraph contacts = new Paragraph(contact, font(7f, Font.NORMAL, TEXT_MUTED));
        contacts.setLeading(9.5f);
        contacts.setSpacingBefore(2);
        left.addElement(contacts);
        table.addCell(left);

        // Right Cell: Official "PAID" Stamp Badge
        PdfPCell stampContainer = new PdfPCell();
        stampContainer.setBorder(Rectangle.NO_BORDER);
        stampContainer.setPadding(3);
        stampContainer.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPTable stamp = new PdfPTable(1);
        stamp.setWidthPercentage(100);

        PdfPCell stampInner = new PdfPCell();
        stampInner.setBackgroundColor(EMERALD_BG);
        stampInner.setBorder(Rectangle.BOX);
        stampInner.setBorderColor(EMERALD_BORDER);
        stampInner.setBorderWidth(1f);
        stampInner.setPaddingTop(4);
        stampInner.setPaddingBottom(4);
        stampInner.setPaddingLeft(6);
        stampInner.setPaddingRight(6);
        stampInner.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph stampHead = new Paragraph("★ OFFICIAL RECEIPT ★", font(6f, Font.BOLD, EMERALD_DARK));
        stampHead.setAlignment(Element.ALIGN_CENTER);
        stampInner.addElement(stampHead);

        Paragraph stampMain = new Paragraph("PAID IN FULL", font(10.5f, Font.BOLD, EMERALD_DARK));
        stampMain.setAlignment(Element.ALIGN_CENTER);
        stampMain.setLeading(12);
        stampInner.addElement(stampMain);

        Paragraph stampSub = new Paragraph("PAYMENT RECEIVED", font(6.5f, Font.BOLD, EMERALD_DARK));
        stampSub.setAlignment(Element.ALIGN_CENTER);
        stampSub.setLeading(8.5f);
        stampInner.addElement(stampSub);

        stamp.addCell(stampInner);
        stampContainer.addElement(stamp);
        table.addCell(stampContainer);

        return table;
    }

    private PdfPTable receiptMeta(Payment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.3f, 1.0f, 0.9f, 1.0f});
        table.setWidthPercentage(100);

        table.addCell(metaPill("RECEIPT NUMBER", payment.getReceiptNumber(), BRAND_NAVY));
        table.addCell(metaPill("PAYMENT DATE", formatDate(payment.getPaymentDate()), TEXT_MAIN));
        table.addCell(metaPill("PAYMENT MODE", payment.getPaymentMode() == PaymentMode.UPI ? "UPI" : "Cash", TEXT_MAIN));
        table.addCell(metaPill("STATUS", "VERIFIED & ACTIVE", EMERALD_DARK));

        return table;
    }

    private PdfPCell metaPill(String label, String value, Color valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_PANEL);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_LINE);
        cell.setBorderWidth(0.75f);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);

        Paragraph lbl = new Paragraph(label, font(5.5f, Font.BOLD, TEXT_MUTED));
        lbl.setLeading(7);
        cell.addElement(lbl);

        Paragraph val = new Paragraph(value, font(8f, Font.BOLD, valueColor));
        val.setLeading(10);
        cell.addElement(val);

        return cell;
    }

    private PdfPTable memberAndScheduleCards(Member member, Payment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.15f, 1.0f});
        table.setWidthPercentage(100);

        // Left Card: Member Details
        PdfPCell leftCard = new PdfPCell();
        leftCard.setBackgroundColor(BG_PANEL);
        leftCard.setBorder(Rectangle.BOX);
        leftCard.setBorderColor(BORDER_LINE);
        leftCard.setBorderWidth(0.5f);
        leftCard.setPadding(6);

        Paragraph cardTitle1 = new Paragraph("MEMBER / STUDENT DETAILS", font(6.5f, Font.BOLD, BRAND_BLUE));
        cardTitle1.setLeading(8);
        leftCard.addElement(cardTitle1);

        Paragraph name = new Paragraph(member.getFullName(), font(9.5f, Font.BOLD, TEXT_MAIN));
        name.setLeading(11.5f);
        name.setSpacingBefore(2);
        leftCard.addElement(name);

        Paragraph phone = new Paragraph("Phone: " + member.getPhone(), font(7f, Font.NORMAL, TEXT_MUTED));
        phone.setLeading(9);
        leftCard.addElement(phone);

        Paragraph plan = new Paragraph("Plan: " + member.getPlanName() + " (" + member.getPlanDurationDays() + " days)", font(7.5f, Font.BOLD, BRAND_NAVY));
        plan.setLeading(9.5f);
        plan.setSpacingBefore(2);
        leftCard.addElement(plan);

        table.addCell(leftCard);

        // Right Card: Membership Validity & Schedule
        PdfPCell rightCard = new PdfPCell();
        rightCard.setBackgroundColor(BG_PANEL);
        rightCard.setBorder(Rectangle.BOX);
        rightCard.setBorderColor(BORDER_LINE);
        rightCard.setBorderWidth(0.5f);
        rightCard.setPadding(6);

        Paragraph cardTitle2 = new Paragraph("MEMBERSHIP VALIDITY & ACCESS", font(6.5f, Font.BOLD, BRAND_BLUE));
        cardTitle2.setLeading(8);
        rightCard.addElement(cardTitle2);

        Paragraph validityRange = new Paragraph("Renewal Validity: " + formatDate(payment.getPaymentDate()) + " to " + formatDate(payment.getNewExpiryDate()), font(7f, Font.NORMAL, TEXT_MAIN));
        validityRange.setLeading(9);
        validityRange.setSpacingBefore(2);
        rightCard.addElement(validityRange);

        Paragraph renewedBadge = new Paragraph("MEMBERSHIP RENEWED UNTIL:\n" + formatDate(payment.getNewExpiryDate()), font(8f, Font.BOLD, EMERALD_DARK));
        renewedBadge.setLeading(10);
        renewedBadge.setSpacingBefore(2);
        rightCard.addElement(renewedBadge);

        Paragraph facility = new Paragraph("Desk check-in & training access authorized", font(6.5f, Font.ITALIC, TEXT_MUTED));
        facility.setLeading(8.5f);
        facility.setSpacingBefore(1);
        rightCard.addElement(facility);

        table.addCell(rightCard);

        return table;
    }

    private PdfPTable lineItems(Tenant tenant, Member member, Payment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.35f, 2.3f, 0.85f, 1.1f});
        table.setWidthPercentage(100);

        // Header Row
        table.addCell(tableHeaderCell("#", Element.ALIGN_CENTER));
        table.addCell(tableHeaderCell("FEE PARTICULARS / PROGRAM DESCRIPTION", Element.ALIGN_LEFT));
        table.addCell(tableHeaderCell("DURATION", Element.ALIGN_CENTER));
        table.addCell(tableHeaderCell("AMOUNT", Element.ALIGN_RIGHT));

        // Line Item 1: Base Membership / Tuition
        table.addCell(tableDataCell("1", font(7.5f, Font.NORMAL, TEXT_MUTED), Element.ALIGN_CENTER, Color.WHITE));

        PdfPCell descCell = new PdfPCell();
        descCell.setBackgroundColor(Color.WHITE);
        descCell.setBorder(Rectangle.BOTTOM);
        descCell.setBorderColor(BORDER_LINE);
        descCell.setPaddingTop(4);
        descCell.setPaddingBottom(4);
        descCell.setPaddingLeft(5);
        descCell.setPaddingRight(5);
        Paragraph itemTitle = new Paragraph(member.getPlanName() + " (Membership / Tuition Fee)", font(7.5f, Font.BOLD, TEXT_MAIN));
        itemTitle.setLeading(9);
        descCell.addElement(itemTitle);
        Paragraph itemSub = new Paragraph("Renewal validity through " + formatDate(payment.getNewExpiryDate()), font(6.5f, Font.NORMAL, TEXT_MUTED));
        itemSub.setLeading(8);
        descCell.addElement(itemSub);
        table.addCell(descCell);

        table.addCell(tableDataCell(member.getPlanDurationDays() + " Days", font(7.5f, Font.NORMAL, TEXT_MAIN), Element.ALIGN_CENTER, Color.WHITE));
        table.addCell(tableDataCell(money(tenant, member.getPlanAmount()), font(7.5f, Font.BOLD, TEXT_MAIN), Element.ALIGN_RIGHT, Color.WHITE));

        // Line Item 2: Promotion Discount if any
        if (payment.getDiscountAmount() != null && payment.getDiscountAmount().signum() > 0) {
            table.addCell(tableDataCell("2", font(7.5f, Font.NORMAL, EMERALD_DARK), Element.ALIGN_CENTER, EMERALD_BG));

            PdfPCell discDescCell = new PdfPCell();
            discDescCell.setBackgroundColor(EMERALD_BG);
            discDescCell.setBorder(Rectangle.BOTTOM);
            discDescCell.setBorderColor(BORDER_LINE);
            discDescCell.setPaddingTop(4);
            discDescCell.setPaddingBottom(4);
            discDescCell.setPaddingLeft(5);
            discDescCell.setPaddingRight(5);
            Paragraph discTitle = new Paragraph("Promotion discount / Special waiver", font(7.5f, Font.BOLD, EMERALD_DARK));
            discTitle.setLeading(9);
            discDescCell.addElement(discTitle);
            Paragraph discSub = new Paragraph("Special promotional offer discount applied", font(6.5f, Font.NORMAL, TEXT_MUTED));
            discSub.setLeading(8);
            discDescCell.addElement(discSub);
            table.addCell(discDescCell);

            table.addCell(tableDataCell("—", font(7.5f, Font.NORMAL, EMERALD_DARK), Element.ALIGN_CENTER, EMERALD_BG));
            table.addCell(tableDataCell("- " + money(tenant, payment.getDiscountAmount()), font(7.5f, Font.BOLD, EMERALD_DARK), Element.ALIGN_RIGHT, EMERALD_BG));
        }

        return table;
    }

    private PdfPCell tableHeaderCell(String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(6.5f, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(BRAND_NAVY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell tableDataCell(String text, Font f, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER_LINE);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPTable amountPanel(Tenant tenant, Member member, Payment payment) throws DocumentException {
        PdfPTable panel = new PdfPTable(new float[]{1.3f, 1.5f});
        panel.setWidthPercentage(100);

        // Left: Payment Mode & Billing Classification
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(BG_PANEL);
        left.setBorder(Rectangle.BOX);
        left.setBorderColor(BORDER_LINE);
        left.setBorderWidth(0.5f);
        left.setPadding(6);

        Paragraph modeTitle = new Paragraph("PAYMENT METHOD & DETAILS", font(6.5f, Font.BOLD, BRAND_BLUE));
        modeTitle.setLeading(8);
        left.addElement(modeTitle);

        String modeText = payment.getPaymentMode() == PaymentMode.UPI ? "UPI / Online Payment" : "Cash Payment";
        Paragraph mode = new Paragraph("Payment Mode: " + modeText, font(7.5f, Font.BOLD, TEXT_MAIN));
        mode.setLeading(9.5f);
        mode.setSpacingBefore(1.5f);
        left.addElement(mode);

        if (payment.getPaymentMode() == PaymentMode.UPI && tenant.getUpiId() != null && !tenant.getUpiId().isBlank()) {
            Paragraph upi = new Paragraph("Destination UPI: " + tenant.getUpiId(), font(7f, Font.NORMAL, TEXT_MUTED));
            upi.setLeading(9);
            left.addElement(upi);
        }

        Paragraph type = new Paragraph("Billing: Member Plan Renewal / Tuition", font(6.5f, Font.ITALIC, TEXT_MUTED));
        type.setLeading(8.5f);
        left.addElement(type);

        panel.addCell(left);

        // Right: Total Amount Banner
        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(BRAND_NAVY);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(7);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph lbl = new Paragraph("TOTAL PAID", font(7f, Font.BOLD, new Color(202, 220, 240)));
        lbl.setLeading(8.5f);
        right.addElement(lbl);

        Paragraph amt = new Paragraph(money(tenant, payment.getAmountPaid()), font(14f, Font.BOLD, Color.WHITE));
        amt.setLeading(16f);
        amt.setSpacingBefore(1f);
        right.addElement(amt);

        Paragraph status = new Paragraph("✓ PAYMENT RECEIVED · PAID IN FULL", font(6.5f, Font.BOLD, new Color(110, 231, 183)));
        status.setLeading(8f);
        status.setSpacingBefore(2f);
        right.addElement(status);

        panel.addCell(right);

        return panel;
    }

    private PdfPTable termsAndRegulations(Payment payment) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_PANEL);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_LINE);
        cell.setBorderWidth(0.5f);
        cell.setPadding(5);

        Paragraph head = new Paragraph("STANDARD GYM & COACHING CENTER REGULATIONS:", font(6f, Font.BOLD, TEXT_MUTED));
        head.setLeading(7.5f);
        cell.addElement(head);

        String terms = "1. Non-Refundable: Fees once remitted are non-refundable and non-transferable under any circumstances.\n" +
                "2. Desk Verification: Present this digital receipt or registered mobile at the center desk for check-in.\n" +
                "3. Validity Notice: Membership & batch access is active strictly until " + formatDate(payment.getNewExpiryDate()) + ". Please renew in advance.\n" +
                "4. Discipline: Members/students must adhere to center schedule, safety norms, and facility etiquette.";
        Paragraph body = new Paragraph(terms, font(5.8f, Font.NORMAL, TEXT_MUTED));
        body.setLeading(7.2f);
        body.setSpacingBefore(1.5f);
        cell.addElement(body);

        table.addCell(cell);
        return table;
    }

    private PdfPTable signatoryBlock(Tenant tenant) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.7f, 1.3f});
        table.setWidthPercentage(100);

        // Left: Digital Generation Note
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(4);

        Paragraph digitalNote = new Paragraph("FeeKeeper SaaS · Authenticated Digital Record", font(6.5f, Font.BOLD, BRAND_BLUE));
        digitalNote.setLeading(8);
        left.addElement(digitalNote);

        Paragraph genNotice = new Paragraph("Issued on behalf of " + tenant.getBusinessName() + ".\nThis is a computer-generated payment receipt; no signature is required.", font(6f, Font.ITALIC, TEXT_MUTED));
        genNotice.setLeading(7.5f);
        left.addElement(genNotice);

        table.addCell(left);

        // Right: Official Desk Validation Stamp Box
        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(Color.WHITE);
        right.setBorder(Rectangle.BOX);
        right.setBorderColor(new Color(148, 163, 184));
        right.setBorderWidth(0.75f);
        right.setPadding(4);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph centerName = new Paragraph(tenant.getBusinessName().toUpperCase(Locale.ENGLISH), font(6f, Font.BOLD, BRAND_NAVY));
        centerName.setAlignment(Element.ALIGN_CENTER);
        centerName.setLeading(7.5f);
        right.addElement(centerName);

        Paragraph stampText = new Paragraph("DESK VERIFIED & RECORDED", font(6.5f, Font.BOLD, EMERALD_DARK));
        stampText.setAlignment(Element.ALIGN_CENTER);
        stampText.setLeading(8f);
        right.addElement(stampText);

        Paragraph authSig = new Paragraph("Authorized Signatory", font(5.5f, Font.ITALIC, TEXT_MUTED));
        authSig.setAlignment(Element.ALIGN_CENTER);
        authSig.setLeading(7f);
        right.addElement(authSig);

        table.addCell(right);

        return table;
    }

    private PdfPTable footer(Tenant tenant) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        String footerText = "Thank you for your payment, " + tenant.getBusinessName() +
                ".  |  Helpline: " + tenant.getPhone() +
                (tenant.getUpiId() != null && !tenant.getUpiId().isBlank() ? "  |  UPI: " + tenant.getUpiId() : "") +
                "  |  Powered by FeeKeeper";

        PdfPCell cell = new PdfPCell(new Phrase(footerText, font(6f, Font.NORMAL, TEXT_MUTED)));
        cell.setBackgroundColor(Color.WHITE);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(BORDER_LINE);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(2);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(cell);
        return table;
    }

    private String money(Tenant tenant, java.math.BigDecimal amount) {
        return tenant.getCurrencySymbol() + " " + tenant.getCurrencyCode() + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Font font(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private String formatDate(LocalDate date) {
        return DATE.format(date);
    }

    /**
     * Page frame event helper to add top decorative branding accent and outer certificate border.
     */
    private static class ReceiptPageFrame extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();

            // 1. Top brand accent bar: Navy header bar with emerald highlight
            cb.setColorFill(BRAND_NAVY);
            cb.rectangle(0, height - 5f, width, 5f);
            cb.fill();

            cb.setColorFill(EMERALD_DARK);
            cb.rectangle(0, height - 7f, 130f, 2f);
            cb.fill();

            // 2. Elegant certificate framing border
            cb.setColorStroke(BORDER_LINE);
            cb.setLineWidth(0.75f);
            cb.roundRectangle(10f, 10f, width - 20f, height - 20f, 4f);
            cb.stroke();

            // 3. Subtle bottom accent line
            cb.setColorFill(BG_HEADER);
            cb.rectangle(10f, 10f, width - 20f, 2f);
            cb.fill();
        }
    }
}
