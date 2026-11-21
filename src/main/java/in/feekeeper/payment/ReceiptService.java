package in.feekeeper.payment;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import in.feekeeper.common.exception.NotFoundException;
import in.feekeeper.member.Member;
import in.feekeeper.member.MemberRepository;
import in.feekeeper.tenant.Tenant;
import in.feekeeper.tenant.TenantContext;
import in.feekeeper.tenant.TenantRepository;
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

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    // ==========================================
    // ANDROMEDA (Classic Prestige Palette)
    // ==========================================
    private static final Color ANDR_NAVY = new Color(15, 23, 42);       // #0F172A
    private static final Color ANDR_BLUE = new Color(30, 58, 138);      // #1E3A8A
    private static final Color ANDR_EMERALD = new Color(4, 120, 87);    // #047857
    private static final Color ANDR_EMERALD_BG = new Color(236, 253, 245); // #ECFDF5
    private static final Color ANDR_EMERALD_BORDER = new Color(110, 231, 183); // #6EE7B7
    private static final Color ANDR_TEXT = new Color(15, 23, 42);
    private static final Color ANDR_MUTED = new Color(100, 116, 139);
    private static final Color ANDR_PANEL = new Color(248, 250, 252);
    private static final Color ANDR_HEADER = new Color(241, 245, 249);
    private static final Color ANDR_LINE = new Color(226, 232, 240);

    // ==========================================
    // NEBULA (Cosmic Athletic Cyber Palette)
    // ==========================================
    private static final Color NEB_DARK = new Color(30, 16, 53);        // #1E1035
    private static final Color NEB_PURPLE = new Color(109, 40, 217);    // #6D28D9
    private static final Color NEB_CYAN = new Color(8, 145, 178);       // #0891B2
    private static final Color NEB_CYAN_BG = new Color(236, 254, 255);  // #ECFEFF
    private static final Color NEB_CYAN_BORDER = new Color(103, 232, 249); // #67E8F9
    private static final Color NEB_TEXT = new Color(30, 16, 53);
    private static final Color NEB_MUTED = new Color(107, 114, 128);
    private static final Color NEB_PANEL = new Color(245, 243, 255);
    private static final Color NEB_HEADER = new Color(237, 233, 254);
    private static final Color NEB_LINE = new Color(221, 214, 254);

    // ==========================================
    // MILKY WAY (Executive Luxury Starlight Palette)
    // ==========================================
    private static final Color MW_OBSIDIAN = new Color(24, 24, 27);     // #18181B
    private static final Color MW_GOLD = new Color(180, 83, 9);         // #B45309
    private static final Color MW_GOLD_DEEP = new Color(146, 64, 14);   // #92400E
    private static final Color MW_GOLD_BG = new Color(254, 243, 199);   // #FEF3C7
    private static final Color MW_GOLD_BORDER = new Color(252, 211, 77);// #FCD34D
    private static final Color MW_TEXT = new Color(24, 24, 27);
    private static final Color MW_MUTED = new Color(120, 113, 108);
    private static final Color MW_PANEL = new Color(250, 250, 249);
    private static final Color MW_HEADER = new Color(245, 245, 244);
    private static final Color MW_LINE = new Color(231, 229, 228);

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
        String theme = tenant.getReceiptTheme() != null ? tenant.getReceiptTheme().trim().toUpperCase(Locale.ROOT) : "ANDROMEDA";
        return switch (theme) {
            case "NEBULA" -> renderNebula(tenant, member, payment);
            case "MILKY_WAY" -> renderMilkyWay(tenant, member, payment);
            default -> renderAndromeda(tenant, member, payment);
        };
    }

    // =========================================================================
    // 1. ANDROMEDA: Classic Academic & Corporate Institutional Style
    // =========================================================================
    private byte[] renderAndromeda(Tenant tenant, Member member, Payment payment) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A5, 22, 22, 18, 18);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new AndromedaPageFrame());
            document.open();

            // 1. Header (Classic Left Academy Info + Right Verified Stamp)
            PdfPTable headerTable = new PdfPTable(new float[]{2.7f, 1.3f});
            headerTable.setWidthPercentage(100);

            PdfPCell left = new PdfPCell();
            left.setBorder(Rectangle.NO_BORDER);
            left.setPadding(4);

            Paragraph orgName = new Paragraph(tenant.getBusinessName(), font(14, Font.BOLD, ANDR_NAVY));
            orgName.setLeading(16);
            left.addElement(orgName);

            Paragraph sub = new Paragraph("ANDROMEDA GALAXY · ACADEMIC & FITNESS DIGITAL RECORD", font(6.5f, Font.BOLD, ANDR_BLUE));
            sub.setLeading(9);
            sub.setSpacingBefore(1);
            left.addElement(sub);

            String contact = "Desk Helpline: " + tenant.getPhone() + "  |  Center Head: " + tenant.getOwnerName() +
                    (tenant.getUpiId() != null && !tenant.getUpiId().isBlank() ? "\nUPI ID: " + tenant.getUpiId() : "");
            Paragraph contacts = new Paragraph(contact, font(7f, Font.NORMAL, ANDR_MUTED));
            contacts.setLeading(9.5f);
            contacts.setSpacingBefore(2);
            left.addElement(contacts);
            headerTable.addCell(left);

            PdfPCell stampContainer = new PdfPCell();
            stampContainer.setBorder(Rectangle.NO_BORDER);
            stampContainer.setPadding(3);
            stampContainer.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPTable stamp = new PdfPTable(1);
            stamp.setWidthPercentage(100);
            PdfPCell stampInner = new PdfPCell();
            stampInner.setBackgroundColor(ANDR_EMERALD_BG);
            stampInner.setBorder(Rectangle.BOX);
            stampInner.setBorderColor(ANDR_EMERALD_BORDER);
            stampInner.setBorderWidth(1f);
            stampInner.setPadding(4);
            stampInner.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph stampHead = new Paragraph("★ OFFICIAL RECEIPT ★", font(6f, Font.BOLD, ANDR_EMERALD));
            stampHead.setAlignment(Element.ALIGN_CENTER);
            stampInner.addElement(stampHead);

            Paragraph stampMain = new Paragraph("PAID IN FULL", font(10.5f, Font.BOLD, ANDR_EMERALD));
            stampMain.setAlignment(Element.ALIGN_CENTER);
            stampMain.setLeading(12);
            stampInner.addElement(stampMain);

            Paragraph stampSub = new Paragraph("PAYMENT RECEIVED", font(6.5f, Font.BOLD, ANDR_EMERALD));
            stampSub.setAlignment(Element.ALIGN_CENTER);
            stampSub.setLeading(8.5f);
            stampInner.addElement(stampSub);

            stamp.addCell(stampInner);
            stampContainer.addElement(stamp);
            headerTable.addCell(stampContainer);

            document.add(headerTable);
            document.add(spacer(3));

            // 2. Metadata Ribbon
            PdfPTable meta = new PdfPTable(new float[]{1.3f, 1.0f, 0.9f, 1.0f});
            meta.setWidthPercentage(100);
            meta.addCell(metaCell("RECEIPT NUMBER", payment.getReceiptNumber(), ANDR_NAVY, ANDR_PANEL, ANDR_LINE));
            meta.addCell(metaCell("PAYMENT DATE", formatDate(payment.getPaymentDate()), ANDR_TEXT, ANDR_PANEL, ANDR_LINE));
            meta.addCell(metaCell("PAYMENT MODE", payment.getPaymentMode() == PaymentMode.UPI ? "UPI" : "Cash", ANDR_TEXT, ANDR_PANEL, ANDR_LINE));
            meta.addCell(metaCell("STATUS", "VERIFIED & ACTIVE", ANDR_EMERALD, ANDR_PANEL, ANDR_LINE));
            document.add(meta);
            document.add(spacer(4));

            // 3. Two Boxed Profile & Validity Cards
            PdfPTable cards = new PdfPTable(new float[]{1.15f, 1.0f});
            cards.setWidthPercentage(100);

            PdfPCell c1 = new PdfPCell();
            c1.setBackgroundColor(ANDR_PANEL);
            c1.setBorder(Rectangle.BOX);
            c1.setBorderColor(ANDR_LINE);
            c1.setBorderWidth(0.5f);
            c1.setPadding(6);
            c1.addElement(new Paragraph("MEMBER / STUDENT DETAILS", font(6.5f, Font.BOLD, ANDR_BLUE)));
            Paragraph mName = new Paragraph(member.getFullName(), font(9.5f, Font.BOLD, ANDR_TEXT));
            mName.setLeading(11.5f);
            mName.setSpacingBefore(2);
            c1.addElement(mName);
            c1.addElement(new Paragraph("Phone: " + member.getPhone(), font(7f, Font.NORMAL, ANDR_MUTED)));
            Paragraph mPlan = new Paragraph("Plan: " + member.getPlanName() + " (" + member.getPlanDurationDays() + " days)", font(7.5f, Font.BOLD, ANDR_NAVY));
            mPlan.setLeading(9.5f);
            mPlan.setSpacingBefore(2);
            c1.addElement(mPlan);
            cards.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setBackgroundColor(ANDR_PANEL);
            c2.setBorder(Rectangle.BOX);
            c2.setBorderColor(ANDR_LINE);
            c2.setBorderWidth(0.5f);
            c2.setPadding(6);
            c2.addElement(new Paragraph("MEMBERSHIP VALIDITY & ACCESS", font(6.5f, Font.BOLD, ANDR_BLUE)));
            c2.addElement(new Paragraph("Renewal Validity: " + formatDate(payment.getPaymentDate()) + " to " + formatDate(payment.getNewExpiryDate()), font(7f, Font.NORMAL, ANDR_TEXT)));
            Paragraph ren = new Paragraph("MEMBERSHIP RENEWED UNTIL:\n" + formatDate(payment.getNewExpiryDate()), font(8f, Font.BOLD, ANDR_EMERALD));
            ren.setLeading(10);
            ren.setSpacingBefore(2);
            c2.addElement(ren);
            c2.addElement(new Paragraph("Desk check-in & training access authorized", font(6.5f, Font.ITALIC, ANDR_MUTED)));
            cards.addCell(c2);

            document.add(cards);
            document.add(spacer(4));

            // 4. Line Items Table
            document.add(createItemsTable(tenant, member, payment, ANDR_NAVY, ANDR_LINE, ANDR_EMERALD, ANDR_EMERALD_BG));
            document.add(spacer(3));

            // 5. Amount Panel
            document.add(createAmountPanel(tenant, member, payment, ANDR_PANEL, ANDR_LINE, ANDR_BLUE, ANDR_NAVY, ANDR_EMERALD_BORDER));
            document.add(spacer(4));

            // 6. Regulations
            document.add(createTerms(payment, ANDR_PANEL, ANDR_LINE, ANDR_MUTED));
            document.add(spacer(3));

            // 7. Signatory
            document.add(createSignatory(tenant, ANDR_BLUE, ANDR_MUTED, ANDR_NAVY, ANDR_EMERALD, "FeeKeeper Andromeda · Authenticated Digital Record"));
            document.add(spacer(2));

            // 8. Footer
            document.add(createFooter(tenant, ANDR_LINE, ANDR_MUTED, "Andromeda Galaxy Edition"));

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate Andromeda receipt", ex);
        }
    }

    // =========================================================================
    // 2. NEBULA: Futuristic Athletic Digital Pass / Cyber Stub Style
    // =========================================================================
    private byte[] renderNebula(Tenant tenant, Member member, Payment payment) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A5, 18, 18, 14, 14);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new NebulaPageFrame());
            document.open();

            // 1. Full-Width Dark Cyber Hero Banner
            PdfPTable hero = new PdfPTable(1);
            hero.setWidthPercentage(100);

            PdfPCell heroCell = new PdfPCell();
            heroCell.setBackgroundColor(NEB_DARK);
            heroCell.setBorder(Rectangle.BOX);
            heroCell.setBorderColor(NEB_CYAN);
            heroCell.setBorderWidth(1f);
            heroCell.setPadding(8);

            Paragraph topBadge = new Paragraph("★ NEBULA ATHLETIC DIGITAL PASS · TOUCHLESS ACCESS AUTHORIZED ★", font(6.5f, Font.BOLD, NEB_CYAN_BORDER));
            topBadge.setAlignment(Element.ALIGN_CENTER);
            topBadge.setLeading(8);
            heroCell.addElement(topBadge);

            Paragraph heroTitle = new Paragraph(tenant.getBusinessName().toUpperCase(Locale.ENGLISH), font(14f, Font.BOLD, Color.WHITE));
            heroTitle.setAlignment(Element.ALIGN_CENTER);
            heroTitle.setLeading(16);
            heroTitle.setSpacingBefore(2);
            heroCell.addElement(heroTitle);

            Paragraph subline = new Paragraph("NEBULA PASS · ATHLETIC FITNESS & COACHING CENTER", font(7f, Font.BOLD, new Color(196, 181, 253)));
            subline.setAlignment(Element.ALIGN_CENTER);
            subline.setLeading(9.5f);
            heroCell.addElement(subline);

            String contactInfo = "Desk Helpline: " + tenant.getPhone() + "  |  Center Head: " + tenant.getOwnerName() +
                    (tenant.getUpiId() != null && !tenant.getUpiId().isBlank() ? "  |  UPI: " + tenant.getUpiId() : "");
            Paragraph contacts = new Paragraph(contactInfo, font(6.5f, Font.NORMAL, new Color(226, 232, 240)));
            contacts.setAlignment(Element.ALIGN_CENTER);
            contacts.setLeading(8.5f);
            contacts.setSpacingBefore(2);
            heroCell.addElement(contacts);

            hero.addCell(heroCell);
            document.add(hero);
            document.add(spacer(3));

            // 2. Cyber Pass Token & Gateway Ribbon (Ticket-Stub Style)
            PdfPTable meta = new PdfPTable(new float[]{1.4f, 1.0f, 0.9f, 1.0f});
            meta.setWidthPercentage(100);
            meta.addCell(metaCell("PASS TOKEN / NO", payment.getReceiptNumber(), NEB_PURPLE, NEB_PANEL, NEB_LINE));
            meta.addCell(metaCell("ISSUED DATE", formatDate(payment.getPaymentDate()), NEB_TEXT, NEB_PANEL, NEB_LINE));
            meta.addCell(metaCell("GATEWAY MODE", payment.getPaymentMode() == PaymentMode.UPI ? "UPI / Digital" : "Cash Desk", NEB_TEXT, NEB_PANEL, NEB_LINE));
            meta.addCell(metaCell("ACCESS STATUS", "PAID IN FULL · ACTIVE", NEB_CYAN, NEB_CYAN_BG, NEB_CYAN_BORDER));
            document.add(meta);
            document.add(spacer(3));

            // 3. Athlete Roster & Facility Turnstile Cards
            PdfPTable cards = new PdfPTable(new float[]{1.2f, 1.0f});
            cards.setWidthPercentage(100);

            PdfPCell c1 = new PdfPCell();
            c1.setBackgroundColor(NEB_PANEL);
            c1.setBorder(Rectangle.BOX);
            c1.setBorderColor(NEB_PURPLE);
            c1.setBorderWidth(0.75f);
            c1.setPadding(6);
            c1.addElement(new Paragraph("ATHLETE / MEMBER ROSTER", font(6.5f, Font.BOLD, NEB_PURPLE)));
            Paragraph mName = new Paragraph(member.getFullName(), font(9.5f, Font.BOLD, NEB_DARK));
            mName.setLeading(11.5f);
            mName.setSpacingBefore(1.5f);
            c1.addElement(mName);
            c1.addElement(new Paragraph("Phone: " + member.getPhone(), font(7f, Font.NORMAL, NEB_MUTED)));
            Paragraph mPlan = new Paragraph("Program: " + member.getPlanName() + " (" + member.getPlanDurationDays() + " days)", font(7.5f, Font.BOLD, NEB_PURPLE));
            mPlan.setLeading(9.5f);
            mPlan.setSpacingBefore(1.5f);
            c1.addElement(mPlan);
            cards.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setBackgroundColor(NEB_CYAN_BG);
            c2.setBorder(Rectangle.BOX);
            c2.setBorderColor(NEB_CYAN);
            c2.setBorderWidth(0.75f);
            c2.setPadding(6);
            c2.addElement(new Paragraph("TURNSTILE & BIOMETRIC ACCESS", font(6.5f, Font.BOLD, NEB_CYAN)));
            c2.addElement(new Paragraph("Validity: " + formatDate(payment.getPaymentDate()) + " to " + formatDate(payment.getNewExpiryDate()), font(7f, Font.NORMAL, NEB_DARK)));
            Paragraph ren = new Paragraph("ACCESS VALID THROUGH:\n" + formatDate(payment.getNewExpiryDate()), font(8f, Font.BOLD, NEB_DARK));
            ren.setLeading(10);
            ren.setSpacingBefore(2);
            c2.addElement(ren);
            c2.addElement(new Paragraph("Touchless turnstile entry & check-in active", font(6.5f, Font.ITALIC, NEB_MUTED)));
            cards.addCell(c2);

            document.add(cards);
            document.add(spacer(3));

            // 4. Line Items Table with Sporty Purple Headers
            document.add(createItemsTable(tenant, member, payment, NEB_PURPLE, NEB_LINE, NEB_CYAN, NEB_CYAN_BG));
            document.add(spacer(3));

            // 5. Cyber Total Amount Banner with Barcode Simulator
            PdfPTable panel = new PdfPTable(new float[]{1.3f, 1.5f});
            panel.setWidthPercentage(100);

            PdfPCell leftPanel = new PdfPCell();
            leftPanel.setBackgroundColor(NEB_PANEL);
            leftPanel.setBorder(Rectangle.BOX);
            leftPanel.setBorderColor(NEB_LINE);
            leftPanel.setBorderWidth(0.5f);
            leftPanel.setPadding(6);
            leftPanel.addElement(new Paragraph("PAYMENT METHOD & TOKEN VALIDATION", font(6.5f, Font.BOLD, NEB_PURPLE)));
            leftPanel.addElement(new Paragraph("Payment Mode: " + (payment.getPaymentMode() == PaymentMode.UPI ? "UPI / Digital Gateway" : "Cash Desk"), font(7.5f, Font.BOLD, NEB_TEXT)));
            if (payment.getPaymentMode() == PaymentMode.UPI && tenant.getUpiId() != null && !tenant.getUpiId().isBlank()) {
                leftPanel.addElement(new Paragraph("Destination UPI: " + tenant.getUpiId(), font(7f, Font.NORMAL, NEB_MUTED)));
            }
            Paragraph secToken = new Paragraph("NEBULA-PASS-ID: " + payment.getReceiptNumber() + "  |  DESK CLEARANCE", font(6f, Font.BOLD, NEB_CYAN));
            secToken.setLeading(8);
            secToken.setSpacingBefore(2);
            leftPanel.addElement(secToken);
            panel.addCell(leftPanel);

            PdfPCell rightPanel = new PdfPCell();
            rightPanel.setBackgroundColor(NEB_DARK);
            rightPanel.setBorder(Rectangle.NO_BORDER);
            rightPanel.setPadding(6);
            rightPanel.setVerticalAlignment(Element.ALIGN_MIDDLE);
            rightPanel.addElement(new Paragraph("TOTAL SETTLED", font(7f, Font.BOLD, NEB_CYAN_BORDER)));
            Paragraph amt = new Paragraph(money(tenant, payment.getAmountPaid()), font(14f, Font.BOLD, Color.WHITE));
            amt.setLeading(16f);
            rightPanel.addElement(amt);
            rightPanel.addElement(new Paragraph("★ PAYMENT RECEIVED · PAID IN FULL ★", font(6.5f, Font.BOLD, NEB_CYAN_BORDER)));
            panel.addCell(rightPanel);

            document.add(panel);
            document.add(spacer(3));

            // 6. Regulations
            document.add(createTerms(payment, NEB_PANEL, NEB_LINE, NEB_MUTED));
            document.add(spacer(2.5f));

            // 7. Touchless Digital Signatory Block
            document.add(createSignatory(tenant, NEB_PURPLE, NEB_MUTED, NEB_DARK, NEB_CYAN, "FeeKeeper Nebula · Cosmic Digital Record"));
            document.add(spacer(2));

            // 8. Footer
            document.add(createFooter(tenant, NEB_LINE, NEB_MUTED, "Nebula Edition"));

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate Nebula receipt", ex);
        }
    }

    // =========================================================================
    // 3. MILKY WAY: Centered Executive Luxury Starlight Certificate Style
    // =========================================================================
    private byte[] renderMilkyWay(Tenant tenant, Member member, Payment payment) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A5, 20, 20, 16, 16);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new MilkyWayPageFrame());
            document.open();

            // 1. Centered Executive Luxury Crest Header
            PdfPTable crestTable = new PdfPTable(1);
            crestTable.setWidthPercentage(100);

            PdfPCell crestCell = new PdfPCell();
            crestCell.setBorder(Rectangle.NO_BORDER);
            crestCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            crestCell.setPadding(4);

            Paragraph starCrest = new Paragraph("✦   ✦   ✦", font(9f, Font.BOLD, MW_GOLD));
            starCrest.setAlignment(Element.ALIGN_CENTER);
            starCrest.setLeading(11);
            crestCell.addElement(starCrest);

            Paragraph bizTitle = new Paragraph(tenant.getBusinessName().toUpperCase(Locale.ENGLISH), font(14.5f, Font.BOLD, MW_OBSIDIAN));
            bizTitle.setAlignment(Element.ALIGN_CENTER);
            bizTitle.setLeading(17);
            bizTitle.setSpacingBefore(1.5f);
            crestCell.addElement(bizTitle);

            Paragraph subtitle = new Paragraph("MILKY WAY STELLAR RECORD · EXECUTIVE GYM & ACADEMY", font(7f, Font.BOLD, MW_GOLD_DEEP));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setLeading(9.5f);
            subtitle.setSpacingBefore(1);
            crestCell.addElement(subtitle);

            String contactText = "Helpline: " + tenant.getPhone() + "  •  Center Head: " + tenant.getOwnerName() +
                    (tenant.getUpiId() != null && !tenant.getUpiId().isBlank() ? "  •  UPI ID: " + tenant.getUpiId() : "");
            Paragraph contacts = new Paragraph(contactText, font(6.8f, Font.NORMAL, MW_MUTED));
            contacts.setAlignment(Element.ALIGN_CENTER);
            contacts.setLeading(8.5f);
            contacts.setSpacingBefore(2);
            crestCell.addElement(contacts);

            Paragraph dividerRibbon = new Paragraph("✦ PRIVILEGED RECORD · SETTLED IN FULL · PAYMENT RECEIVED ✦", font(6.5f, Font.BOLD, MW_GOLD));
            dividerRibbon.setAlignment(Element.ALIGN_CENTER);
            dividerRibbon.setLeading(8.5f);
            dividerRibbon.setSpacingBefore(2);
            crestCell.addElement(dividerRibbon);

            crestTable.addCell(crestCell);
            document.add(crestTable);
            document.add(spacer(3));

            // 2. Minimalist Gold Metadata Ribbon
            PdfPTable meta = new PdfPTable(new float[]{1.3f, 1.0f, 0.9f, 1.0f});
            meta.setWidthPercentage(100);
            meta.addCell(metaCell("REGISTRY CERTIFICATE", payment.getReceiptNumber(), MW_OBSIDIAN, MW_PANEL, MW_GOLD_BORDER));
            meta.addCell(metaCell("PAYMENT DATE", formatDate(payment.getPaymentDate()), MW_TEXT, MW_PANEL, MW_GOLD_BORDER));
            meta.addCell(metaCell("SETTLEMENT MODE", payment.getPaymentMode() == PaymentMode.UPI ? "UPI / Digital" : "Cash Treasury", MW_TEXT, MW_PANEL, MW_GOLD_BORDER));
            meta.addCell(metaCell("PRIVILEGE STATUS", "PAID IN FULL · PRIVILEGED", MW_GOLD_DEEP, MW_GOLD_BG, MW_GOLD_BORDER));
            document.add(meta);
            document.add(spacer(3.5f));

            // 3. Distinguished Patron & Executive Privilege Schedule
            PdfPTable cards = new PdfPTable(new float[]{1.15f, 1.0f});
            cards.setWidthPercentage(100);

            PdfPCell c1 = new PdfPCell();
            c1.setBackgroundColor(new Color(255, 253, 247)); // luxury parchment
            c1.setBorder(Rectangle.BOX);
            c1.setBorderColor(MW_GOLD_BORDER);
            c1.setBorderWidth(0.75f);
            c1.setPadding(6);
            c1.addElement(new Paragraph("DISTINGUISHED PATRON DETAILS", font(6.5f, Font.BOLD, MW_GOLD)));
            Paragraph mName = new Paragraph(member.getFullName(), font(9.5f, Font.BOLD, MW_OBSIDIAN));
            mName.setLeading(11.5f);
            mName.setSpacingBefore(1.5f);
            c1.addElement(mName);
            c1.addElement(new Paragraph("Phone: " + member.getPhone(), font(7f, Font.NORMAL, MW_MUTED)));
            Paragraph mPlan = new Paragraph("Membership: " + member.getPlanName() + " (" + member.getPlanDurationDays() + " days)", font(7.5f, Font.BOLD, MW_OBSIDIAN));
            mPlan.setLeading(9.5f);
            mPlan.setSpacingBefore(1.5f);
            c1.addElement(mPlan);
            cards.addCell(c1);

            PdfPCell c2 = new PdfPCell();
            c2.setBackgroundColor(new Color(255, 253, 247));
            c2.setBorder(Rectangle.BOX);
            c2.setBorderColor(MW_GOLD_BORDER);
            c2.setBorderWidth(0.75f);
            c2.setPadding(6);
            c2.addElement(new Paragraph("HONORED PRIVILEGE VALIDITY", font(6.5f, Font.BOLD, MW_GOLD)));
            c2.addElement(new Paragraph("Renewal Validity: " + formatDate(payment.getPaymentDate()) + " to " + formatDate(payment.getNewExpiryDate()), font(7f, Font.NORMAL, MW_TEXT)));
            Paragraph ren = new Paragraph("STELLAR ACCESS VALID UNTIL:\n" + formatDate(payment.getNewExpiryDate()), font(8f, Font.BOLD, MW_GOLD_DEEP));
            ren.setLeading(10);
            ren.setSpacingBefore(2);
            c2.addElement(ren);
            c2.addElement(new Paragraph("⭐ Executive training & facility privilege active", font(6.5f, Font.ITALIC, MW_MUTED)));
            cards.addCell(c2);

            document.add(cards);
            document.add(spacer(3.5f));

            // 4. Line Items Table with Obsidian & Champagne Headers
            document.add(createItemsTable(tenant, member, payment, MW_OBSIDIAN, MW_LINE, MW_GOLD_DEEP, MW_GOLD_BG));
            document.add(spacer(3));

            // 5. Gilded Investment Block
            PdfPTable panel = new PdfPTable(new float[]{1.3f, 1.5f});
            panel.setWidthPercentage(100);

            PdfPCell leftPanel = new PdfPCell();
            leftPanel.setBackgroundColor(MW_PANEL);
            leftPanel.setBorder(Rectangle.BOX);
            leftPanel.setBorderColor(MW_LINE);
            leftPanel.setBorderWidth(0.5f);
            leftPanel.setPadding(6);
            leftPanel.addElement(new Paragraph("PAYMENT METHOD & DETAILS", font(6.5f, Font.BOLD, MW_GOLD)));
            leftPanel.addElement(new Paragraph("Payment Mode: " + (payment.getPaymentMode() == PaymentMode.UPI ? "UPI / Digital Settlement" : "Cash Treasury"), font(7.5f, Font.BOLD, MW_TEXT)));
            if (payment.getPaymentMode() == PaymentMode.UPI && tenant.getUpiId() != null && !tenant.getUpiId().isBlank()) {
                leftPanel.addElement(new Paragraph("Destination UPI: " + tenant.getUpiId(), font(7f, Font.NORMAL, MW_MUTED)));
            }
            leftPanel.addElement(new Paragraph("Classification: Executive Member Plan Renewal / Tuition", font(6.5f, Font.ITALIC, MW_MUTED)));
            panel.addCell(leftPanel);

            PdfPCell rightPanel = new PdfPCell();
            rightPanel.setBackgroundColor(MW_OBSIDIAN);
            rightPanel.setBorder(Rectangle.NO_BORDER);
            rightPanel.setPadding(6);
            rightPanel.setVerticalAlignment(Element.ALIGN_MIDDLE);
            rightPanel.addElement(new Paragraph("TOTAL PAID / PRIVILEGED INVESTMENT", font(6.5f, Font.BOLD, MW_GOLD_BORDER)));
            Paragraph amt = new Paragraph(money(tenant, payment.getAmountPaid()), font(14f, Font.BOLD, Color.WHITE));
            amt.setLeading(16f);
            rightPanel.addElement(amt);
            rightPanel.addElement(new Paragraph("✦ PAYMENT RECEIVED · PAID IN FULL ✦", font(6.5f, Font.BOLD, MW_GOLD_BORDER)));
            panel.addCell(rightPanel);

            document.add(panel);
            document.add(spacer(3));

            // 6. Regulations
            document.add(createTerms(payment, MW_PANEL, MW_LINE, MW_MUTED));
            document.add(spacer(2.5f));

            // 7. Executive Signatory Block
            document.add(createSignatory(tenant, MW_GOLD, MW_MUTED, MW_OBSIDIAN, MW_GOLD_DEEP, "FeeKeeper Milky Way · Stellar Executive Record"));
            document.add(spacer(2));

            // 8. Footer
            document.add(createFooter(tenant, MW_LINE, MW_MUTED, "Milky Way Starlight Edition"));

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not generate Milky Way receipt", ex);
        }
    }

    // =========================================================================
    // Common PDF Components Helpers
    // =========================================================================
    private PdfPCell metaCell(String label, String value, Color valueColor, Color bg, Color border) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(border);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);

        Paragraph lbl = new Paragraph(label, font(5.5f, Font.BOLD, ANDR_MUTED));
        lbl.setLeading(7);
        cell.addElement(lbl);

        Paragraph val = new Paragraph(value, font(8f, Font.BOLD, valueColor));
        val.setLeading(10);
        cell.addElement(val);

        return cell;
    }

    private PdfPTable createItemsTable(Tenant tenant, Member member, Payment payment,
                                       Color headerBg, Color lineBorder, Color discColor, Color discBg) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.35f, 2.3f, 0.85f, 1.1f});
        table.setWidthPercentage(100);

        table.addCell(tableHeaderCell("#", Element.ALIGN_CENTER, headerBg));
        table.addCell(tableHeaderCell("FEE PARTICULARS / PROGRAM DESCRIPTION", Element.ALIGN_LEFT, headerBg));
        table.addCell(tableHeaderCell("DURATION", Element.ALIGN_CENTER, headerBg));
        table.addCell(tableHeaderCell("AMOUNT", Element.ALIGN_RIGHT, headerBg));

        table.addCell(tableDataCell("1", font(7.5f, Font.NORMAL, ANDR_MUTED), Element.ALIGN_CENTER, Color.WHITE, lineBorder));

        PdfPCell descCell = new PdfPCell();
        descCell.setBackgroundColor(Color.WHITE);
        descCell.setBorder(Rectangle.BOTTOM);
        descCell.setBorderColor(lineBorder);
        descCell.setPaddingTop(4);
        descCell.setPaddingBottom(4);
        descCell.setPaddingLeft(5);
        descCell.setPaddingRight(5);
        Paragraph itemTitle = new Paragraph(member.getPlanName() + " (Membership / Tuition Fee)", font(7.5f, Font.BOLD, ANDR_TEXT));
        itemTitle.setLeading(9);
        descCell.addElement(itemTitle);
        Paragraph itemSub = new Paragraph("Renewal validity through " + formatDate(payment.getNewExpiryDate()), font(6.5f, Font.NORMAL, ANDR_MUTED));
        itemSub.setLeading(8);
        descCell.addElement(itemSub);
        table.addCell(descCell);

        table.addCell(tableDataCell(member.getPlanDurationDays() + " Days", font(7.5f, Font.NORMAL, ANDR_TEXT), Element.ALIGN_CENTER, Color.WHITE, lineBorder));
        table.addCell(tableDataCell(money(tenant, member.getPlanAmount()), font(7.5f, Font.BOLD, ANDR_TEXT), Element.ALIGN_RIGHT, Color.WHITE, lineBorder));

        if (payment.getDiscountAmount() != null && payment.getDiscountAmount().signum() > 0) {
            table.addCell(tableDataCell("2", font(7.5f, Font.NORMAL, discColor), Element.ALIGN_CENTER, discBg, lineBorder));

            PdfPCell discDescCell = new PdfPCell();
            discDescCell.setBackgroundColor(discBg);
            discDescCell.setBorder(Rectangle.BOTTOM);
            discDescCell.setBorderColor(lineBorder);
            discDescCell.setPaddingTop(4);
            discDescCell.setPaddingBottom(4);
            discDescCell.setPaddingLeft(5);
            discDescCell.setPaddingRight(5);
            Paragraph discTitle = new Paragraph("Promotion discount / Special waiver", font(7.5f, Font.BOLD, discColor));
            discTitle.setLeading(9);
            discDescCell.addElement(discTitle);
            Paragraph discSub = new Paragraph("Special promotional offer discount applied", font(6.5f, Font.NORMAL, ANDR_MUTED));
            discSub.setLeading(8);
            discDescCell.addElement(discSub);
            table.addCell(discDescCell);

            table.addCell(tableDataCell("—", font(7.5f, Font.NORMAL, discColor), Element.ALIGN_CENTER, discBg, lineBorder));
            table.addCell(tableDataCell("- " + money(tenant, payment.getDiscountAmount()), font(7.5f, Font.BOLD, discColor), Element.ALIGN_RIGHT, discBg, lineBorder));
        }

        return table;
    }

    private PdfPCell tableHeaderCell(String text, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(6.5f, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell tableDataCell(String text, Font f, int align, Color bg, Color border) {
        PdfPCell cell = new PdfPCell(new Phrase(text, f));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(border);
        cell.setPaddingTop(4);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(5);
        cell.setPaddingRight(5);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPTable createAmountPanel(Tenant tenant, Member member, Payment payment,
                                        Color bgPanel, Color lineBorder, Color titleColor, Color bannerBg, Color statusColor) throws DocumentException {
        PdfPTable panel = new PdfPTable(new float[]{1.3f, 1.5f});
        panel.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(bgPanel);
        left.setBorder(Rectangle.BOX);
        left.setBorderColor(lineBorder);
        left.setBorderWidth(0.5f);
        left.setPadding(6);

        Paragraph modeTitle = new Paragraph("PAYMENT METHOD & DETAILS", font(6.5f, Font.BOLD, titleColor));
        modeTitle.setLeading(8);
        left.addElement(modeTitle);

        String modeText = payment.getPaymentMode() == PaymentMode.UPI ? "UPI / Online Payment" : "Cash Payment";
        Paragraph mode = new Paragraph("Payment Mode: " + modeText, font(7.5f, Font.BOLD, ANDR_TEXT));
        mode.setLeading(9.5f);
        mode.setSpacingBefore(1.5f);
        left.addElement(mode);

        if (payment.getPaymentMode() == PaymentMode.UPI && tenant.getUpiId() != null && !tenant.getUpiId().isBlank()) {
            Paragraph upi = new Paragraph("Destination UPI: " + tenant.getUpiId(), font(7f, Font.NORMAL, ANDR_MUTED));
            upi.setLeading(9);
            left.addElement(upi);
        }

        Paragraph type = new Paragraph("Billing: Member Plan Renewal / Tuition", font(6.5f, Font.ITALIC, ANDR_MUTED));
        type.setLeading(8.5f);
        left.addElement(type);
        panel.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(bannerBg);
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

        Paragraph status = new Paragraph("✓ PAYMENT RECEIVED · PAID IN FULL", font(6.5f, Font.BOLD, statusColor));
        status.setLeading(8f);
        status.setSpacingBefore(2f);
        right.addElement(status);

        panel.addCell(right);
        return panel;
    }

    private PdfPTable createTerms(Payment payment, Color bg, Color border, Color textMuted) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(border);
        cell.setBorderWidth(0.5f);
        cell.setPadding(4.5f);

        Paragraph head = new Paragraph("STANDARD GYM & COACHING CENTER REGULATIONS:", font(6f, Font.BOLD, textMuted));
        head.setLeading(7.5f);
        cell.addElement(head);

        String terms = "1. Non-Refundable: Fees once remitted are non-refundable and non-transferable under any circumstances.\n" +
                "2. Desk Verification: Present this digital receipt or registered mobile at the center desk for check-in.\n" +
                "3. Validity Notice: Membership & batch access is active strictly until " + formatDate(payment.getNewExpiryDate()) + ". Please renew in advance.\n" +
                "4. Discipline: Members/students must adhere to center schedule, safety norms, and facility etiquette.";
        Paragraph body = new Paragraph(terms, font(5.6f, Font.NORMAL, textMuted));
        body.setLeading(7f);
        body.setSpacingBefore(1f);
        cell.addElement(body);

        table.addCell(cell);
        return table;
    }

    private PdfPTable createSignatory(Tenant tenant, Color titleColor, Color textMuted,
                                      Color nameColor, Color stampColor, String digitalTitle) {
        PdfPTable table = new PdfPTable(new float[]{1.7f, 1.3f});
        table.setWidthPercentage(100);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(4);

        Paragraph digitalNote = new Paragraph(digitalTitle, font(6.5f, Font.BOLD, titleColor));
        digitalNote.setLeading(8);
        left.addElement(digitalNote);

        Paragraph genNotice = new Paragraph("Issued on behalf of " + tenant.getBusinessName() + ".\nThis is a computer-generated payment receipt; no signature is required.", font(6f, Font.ITALIC, textMuted));
        genNotice.setLeading(7.5f);
        left.addElement(genNotice);
        table.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(Color.WHITE);
        right.setBorder(Rectangle.BOX);
        right.setBorderColor(new Color(148, 163, 184));
        right.setBorderWidth(0.75f);
        right.setPadding(3.5f);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph centerName = new Paragraph(tenant.getBusinessName().toUpperCase(Locale.ENGLISH), font(6f, Font.BOLD, nameColor));
        centerName.setAlignment(Element.ALIGN_CENTER);
        centerName.setLeading(7.5f);
        right.addElement(centerName);

        Paragraph stampText = new Paragraph("DESK VERIFIED & RECORDED", font(6.5f, Font.BOLD, stampColor));
        stampText.setAlignment(Element.ALIGN_CENTER);
        stampText.setLeading(8f);
        right.addElement(stampText);

        Paragraph authSig = new Paragraph("Authorized Signatory", font(5.5f, Font.ITALIC, textMuted));
        authSig.setAlignment(Element.ALIGN_CENTER);
        authSig.setLeading(7f);
        right.addElement(authSig);
        table.addCell(right);

        return table;
    }

    private PdfPTable createFooter(Tenant tenant, Color borderLine, Color textMuted, String editionName) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        String footerText = "Thank you for your payment, " + tenant.getBusinessName() +
                ".  |  Helpline: " + tenant.getPhone() +
                (tenant.getUpiId() != null && !tenant.getUpiId().isBlank() ? "  |  UPI: " + tenant.getUpiId() : "") +
                "  |  Powered by FeeKeeper · " + editionName;

        PdfPCell cell = new PdfPCell(new Phrase(footerText, font(5.8f, Font.NORMAL, textMuted)));
        cell.setBackgroundColor(Color.WHITE);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(borderLine);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(1);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(cell);
        return table;
    }

    private Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ", font(height, Font.NORMAL, ANDR_MUTED));
        p.setLeading(height);
        return p;
    }

    private String money(Tenant tenant, java.math.BigDecimal amount) {
        return tenant.getCurrencySymbol() + " " + tenant.getCurrencyCode() + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Font font(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "—";
        return DATE.format(date);
    }

    // =========================================================================
    // PAGE FRAME EVENT HELPERS (Custom Styling for Each Galaxy Theme)
    // =========================================================================

    /**
     * Andromeda Page Frame: Classical institutional double border with formal Navy & Emerald header stripe.
     */
    private static class AndromedaPageFrame extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();

            cb.setColorFill(ANDR_NAVY);
            cb.rectangle(0, height - 5f, width, 5f);
            cb.fill();

            cb.setColorFill(ANDR_EMERALD);
            cb.rectangle(0, height - 7f, 130f, 2f);
            cb.fill();

            cb.setColorStroke(ANDR_LINE);
            cb.setLineWidth(0.75f);
            cb.roundRectangle(10f, 10f, width - 20f, height - 20f, 4f);
            cb.stroke();

            cb.setColorFill(ANDR_HEADER);
            cb.rectangle(10f, 10f, width - 20f, 2f);
            cb.fill();
        }
    }

    /**
     * Nebula Page Frame: High-tech cyber geometric border with 4 corner tick brackets and neon cyan accent line.
     */
    private static class NebulaPageFrame extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();

            // Top Cyan & Violet Cyber Accent
            cb.setColorFill(NEB_DARK);
            cb.rectangle(0, height - 5f, width, 5f);
            cb.fill();

            cb.setColorFill(NEB_CYAN);
            cb.rectangle(0, height - 7f, width, 2f);
            cb.fill();

            // Outer Lavender Cyber Border
            cb.setColorStroke(NEB_LINE);
            cb.setLineWidth(1f);
            cb.roundRectangle(8f, 8f, width - 16f, height - 16f, 6f);
            cb.stroke();

            // 4 Futuristic Corner Bracket Ticks
            cb.setColorStroke(NEB_CYAN);
            cb.setLineWidth(1.5f);

            // Top-Left corner
            cb.moveTo(12f, height - 20f); cb.lineTo(12f, height - 12f); cb.lineTo(20f, height - 12f); cb.stroke();
            // Top-Right corner
            cb.moveTo(width - 20f, height - 12f); cb.lineTo(width - 12f, height - 12f); cb.lineTo(width - 12f, height - 20f); cb.stroke();
            // Bottom-Left corner
            cb.moveTo(12f, 20f); cb.lineTo(12f, 12f); cb.lineTo(20f, 12f); cb.stroke();
            // Bottom-Right corner
            cb.moveTo(width - 20f, 12f); cb.lineTo(width - 12f, 12f); cb.lineTo(width - 12f, 20f); cb.stroke();

            // Bottom Neon Accent Bar
            cb.setColorFill(NEB_PURPLE);
            cb.rectangle(8f, 8f, width - 16f, 2f);
            cb.fill();
        }
    }

    /**
     * Milky Way Page Frame: Luxury Gold Double Hairline Border with 4 Diamond/Star Corner Motifs.
     */
    private static class MilkyWayPageFrame extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContentUnder();
            float width = document.getPageSize().getWidth();
            float height = document.getPageSize().getHeight();

            // Top Gold Starlight Ribbon
            cb.setColorFill(MW_OBSIDIAN);
            cb.rectangle(0, height - 4f, width, 4f);
            cb.fill();

            cb.setColorFill(MW_GOLD);
            cb.rectangle(0, height - 6f, width, 2f);
            cb.fill();

            // Outer Gold Frame
            cb.setColorStroke(MW_GOLD_BORDER);
            cb.setLineWidth(0.75f);
            cb.rectangle(9f, 9f, width - 18f, height - 18f);
            cb.stroke();

            // Inner Fine Gold Hairline
            cb.setColorStroke(new Color(254, 243, 199));
            cb.setLineWidth(0.5f);
            cb.rectangle(12f, 12f, width - 24f, height - 24f);
            cb.stroke();

            // 4 Gold Corner Stars (diamonds)
            cb.setColorFill(MW_GOLD);
            drawDiamond(cb, 10.5f, height - 10.5f, 2f);
            drawDiamond(cb, width - 10.5f, height - 10.5f, 2f);
            drawDiamond(cb, 10.5f, 10.5f, 2f);
            drawDiamond(cb, width - 10.5f, 10.5f, 2f);

            // Bottom Amber Bar
            cb.setColorFill(MW_GOLD_BG);
            cb.rectangle(9f, 9f, width - 18f, 2f);
            cb.fill();
        }

        private void drawDiamond(PdfContentByte cb, float cx, float cy, float r) {
            cb.moveTo(cx, cy + r);
            cb.lineTo(cx + r, cy);
            cb.lineTo(cx, cy - r);
            cb.lineTo(cx - r, cy);
            cb.closePath();
            cb.fill();
        }
    }
}
