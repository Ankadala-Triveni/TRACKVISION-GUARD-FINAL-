package com.railguard.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.railguard.entity.Detection;
import com.railguard.entity.Inspection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service generating official, downloadable PDF engineering inspection reports
 * for TRACKVISION GUARD.
 */
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final com.railguard.repository.DetectionRepository detectionRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Corporate & Railway Theme Palette
    private static final Color PRIMARY_BLUE = new Color(11, 87, 208);
    private static final Color DARK_HEADER = new Color(20, 24, 33);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BG_LIGHT = new Color(248, 250, 252);
    private static final Color BORDER_GRAY = new Color(226, 232, 240);
    private static final Color ALERT_RED = new Color(220, 38, 38);
    private static final Color ALERT_ORANGE = new Color(234, 88, 12);
    private static final Color ALERT_GREEN = new Color(22, 163, 74);

    public byte[] generateInspectionReport(Inspection inspection) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent());
            document.open();

            // 1. Title & Branding Header
            addReportHeader(document, inspection);

            // 2. Inspection Metadata Grid
            addInspectionDetailsTable(document, inspection);

            document.add(new Paragraph(" "));

            // 3. AI Detections Table
            addDetectionsSection(document, inspection);

            document.add(new Paragraph(" "));

            // 4. Engineering Risk, Causes & Recommendations
            addEngineeringGuidance(document, inspection);

            document.add(new Paragraph(" "));

            // 5. Official Disclaimer & Sign-off
            addSignoffAndDisclaimer(document, inspection);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF inspection report: " + e.getMessage(), e);
        }
    }

    private void addReportHeader(Document doc, Inspection insp) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{70f, 30f});

        // Left: Branding Title & Subtitle
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPaddingBottom(12f);

        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_BLUE);
        Paragraph brandPara = new Paragraph("TRACKVISION GUARD", brandFont);
        brandPara.setSpacingAfter(2f);
        leftCell.addElement(brandPara);

        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_MUTED);
        Paragraph subPara = new Paragraph("Railway Track Defect Detection and Safety Monitoring System", subtitleFont);
        leftCell.addElement(subPara);

        headerTable.addCell(leftCell);

        // Right: Report Number & Date
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setPaddingBottom(12f);

        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_HEADER);
        Font metaValFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        Paragraph repNum = new Paragraph("REPORT #" + (insp.getInspectionNumber() != null ? insp.getInspectionNumber() : "INS-" + insp.getId()), metaFont);
        repNum.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(repNum);

        Paragraph repDate = new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FMT), metaValFont);
        repDate.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(repDate);

        headerTable.addCell(rightCell);
        doc.add(headerTable);

        // Horizontal divider line
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(PRIMARY_BLUE);
        lineCell.setFixedHeight(2.5f);
        lineCell.setBorder(Rectangle.NO_BORDER);
        divider.addCell(lineCell);
        doc.add(divider);

        doc.add(new Paragraph(" "));
    }

    private void addInspectionDetailsTable(Document doc, Inspection insp) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK_HEADER);
        Paragraph secHeader = new Paragraph("1. Inspection Telemetry & Location Metadata", sectionFont);
        secHeader.setSpacingAfter(6f);
        doc.add(secHeader);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{22f, 28f, 22f, 28f});

        addMetaRow(table, "Inspection ID:", String.valueOf(insp.getId()), "Railway Zone:", defaultVal(insp.getRailwayZone(), "Not specified"));
        addMetaRow(table, "Track Section:", defaultVal(insp.getTrackSection(), "General Track"), "Location Desc:", defaultVal(insp.getLocation(), "Unspecified"));

        String gpsStr = (insp.getLatitude() != null && insp.getLongitude() != null)
                ? String.format("%.5f, %.5f", insp.getLatitude(), insp.getLongitude())
                : "No GPS Recorded";
        addMetaRow(table, "Coordinates:", gpsStr, "Analysis Status:", insp.getStatus() != null ? insp.getStatus().name() : "COMPLETED");

        String riskScoreStr = (insp.getRiskScore() != null) ? String.format("%.2f / 100", insp.getRiskScore()) : "0.00 / 100";
        String riskLvlStr = (insp.getRiskLevel() != null) ? insp.getRiskLevel().name() : "LOW";
        addMetaRow(table, "Overall Risk Level:", riskLvlStr, "Risk Score:", riskScoreStr);

        doc.add(table);
    }

    private void addMetaRow(PdfPTable table, String label1, String val1, String label2, String val2) {
        Font lblFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_MUTED);
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, DARK_HEADER);

        PdfPCell c1 = new PdfPCell(new Phrase(label1, lblFont));
        c1.setBackgroundColor(BG_LIGHT);
        c1.setBorderColor(BORDER_GRAY);
        c1.setPadding(5f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(val1, valFont));
        c2.setBorderColor(BORDER_GRAY);
        c2.setPadding(5f);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(label2, lblFont));
        c3.setBackgroundColor(BG_LIGHT);
        c3.setBorderColor(BORDER_GRAY);
        c3.setPadding(5f);
        table.addCell(c3);

        PdfPCell c4 = new PdfPCell(new Phrase(val2, valFont));
        c4.setBorderColor(BORDER_GRAY);
        c4.setPadding(5f);
        table.addCell(c4);
    }

    private void addDetectionsSection(Document doc, Inspection insp) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK_HEADER);
        Paragraph secHeader = new Paragraph("2. AI Defect Detection Analysis & Findings", sectionFont);
        secHeader.setSpacingAfter(6f);
        doc.add(secHeader);

        List<Detection> detections = detectionRepository.findByInspectionId(insp.getId());

        if (detections == null || detections.isEmpty()) {
            PdfPTable cleanTable = new PdfPTable(1);
            cleanTable.setWidthPercentage(100);
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(new Color(240, 253, 244));
            cell.setBorderColor(ALERT_GREEN);
            cell.setPadding(10f);

            Font greenFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, ALERT_GREEN);
            Paragraph p = new Paragraph("No supported defect confidently detected. This does not guarantee that the track is defect-free.", greenFont);
            cell.addElement(p);
            cleanTable.addCell(cell);
            doc.add(cleanTable);
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{10f, 32f, 18f, 18f, 22f});

        Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
        String[] headers = {"#", "Defect Category", "Confidence", "Severity", "Bounding Box (px)"};
        for (String h : headers) {
            PdfPCell th = new PdfPCell(new Phrase(h, thFont));
            th.setBackgroundColor(DARK_HEADER);
            th.setBorderColor(DARK_HEADER);
            th.setPadding(6f);
            table.addCell(th);
        }

        Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, DARK_HEADER);
        Font tdBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, DARK_HEADER);

        int idx = 1;
        for (Detection d : detections) {
            Color rowBg = (idx % 2 == 0) ? BG_LIGHT : Color.WHITE;

            table.addCell(createCell(String.valueOf(idx), tdFont, rowBg, Element.ALIGN_CENTER));
            table.addCell(createCell(d.getDefectClass(), tdBold, rowBg, Element.ALIGN_LEFT));

            String confStr = String.format("%.1f%%", d.getConfidence() * 100);
            table.addCell(createCell(confStr, tdFont, rowBg, Element.ALIGN_CENTER));

            String sevStr = (d.getSeverity() != null) ? d.getSeverity().name() : "MEDIUM";
            Color sevColor = sevStr.equals("CRITICAL") || sevStr.equals("HIGH") ? ALERT_RED : (sevStr.equals("MEDIUM") ? ALERT_ORANGE : ALERT_GREEN);
            Font sevFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, sevColor);
            table.addCell(createCell(sevStr, sevFont, rowBg, Element.ALIGN_CENTER));

            String bboxStr = String.format("[%.0f, %.0f, %.0f, %.0f]", d.getXMin(), d.getYMin(), d.getXMax(), d.getYMax());
            table.addCell(createCell(bboxStr, tdFont, rowBg, Element.ALIGN_CENTER));

            idx++;
        }

        doc.add(table);
    }

    private PdfPCell createCell(String text, Font font, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(BORDER_GRAY);
        c.setHorizontalAlignment(align);
        c.setPadding(5f);
        return c;
    }

    private void addEngineeringGuidance(Document doc, Inspection insp) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK_HEADER);
        Paragraph secHeader = new Paragraph("3. Engineering Risk Assessment & Recommendations", sectionFont);
        secHeader.setSpacingAfter(6f);
        doc.add(secHeader);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50f, 50f});

        Font bTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, PRIMARY_BLUE);
        Font bBodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.DARK_GRAY);

        // Card 1: Potential Safety Risks
        PdfPCell riskCell = new PdfPCell();
        riskCell.setBorderColor(BORDER_GRAY);
        riskCell.setPadding(8f);
        riskCell.addElement(new Paragraph("Potential Safety Risks:", bTitleFont));
        riskCell.addElement(new Paragraph("• Progressive rail deterioration and accelerated surface spalling.\n" +
                "• Micro-crack propagation leading to potential joint discontinuity.\n" +
                "• Increased dynamic dynamic impact loads on railway rolling stock.\n" +
                "• Potential necessity for temporary operational speed restrictions.", bBodyFont));
        table.addCell(riskCell);

        // Card 2: Recommended Precautions
        PdfPCell precCell = new PdfPCell();
        precCell.setBorderColor(BORDER_GRAY);
        precCell.setPadding(8f);
        precCell.addElement(new Paragraph("Immediate Precautions:", bTitleFont));
        precCell.addElement(new Paragraph("• Arrange immediate on-site inspection by qualified permanent way engineer.\n" +
                "• Verify integrity of nearby fasteners, fishplates, and sleepers.\n" +
                "• Follow authorized railway protocols before authorizing line operations.\n" +
                "• Never allow unqualified personnel to enter active track lines.", bBodyFont));
        table.addCell(precCell);

        // Card 3: Maintenance Actions
        PdfPCell maintCell = new PdfPCell();
        maintCell.setBorderColor(BORDER_GRAY);
        maintCell.setPadding(8f);
        maintCell.addElement(new Paragraph("Recommended Maintenance Actions:", bTitleFont));
        maintCell.addElement(new Paragraph("• Conduct ultrasonic testing (UT) or eddy current testing for subsurface depth.\n" +
                "• Evaluate grinding, weld-repair, or section replacement per railway standards.\n" +
                "• Record detailed geometric dimensions and monitor defect progression.", bBodyFont));
        table.addCell(maintCell);

        // Card 4: Preventive Strategies
        PdfPCell prevCell = new PdfPCell();
        prevCell.setBorderColor(BORDER_GRAY);
        prevCell.setPadding(8f);
        prevCell.addElement(new Paragraph("Long-Term Prevention:", bTitleFont));
        prevCell.addElement(new Paragraph("• Implement cyclic rail profile grinding and friction management.\n" +
                "• Maintain adequate track drainage and corrosion inhibitor treatments.\n" +
                "• Perform periodic ultrasonic rail testing and track geometry monitoring.", bBodyFont));
        table.addCell(prevCell);

        doc.add(table);
    }

    private void addSignoffAndDisclaimer(Document doc, Inspection insp) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, DARK_HEADER);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXT_MUTED);

        Paragraph disclaimer = new Paragraph("STATUTORY DISCLAIMER & ENGINEERING LIMITATIONS:\n" +
                "This document is an AI-assisted preliminary inspection generated by TRACKVISION GUARD using the YOLO11 Railway Inspection Model. " +
                "Surface image analysis does not confirm subsurface integrity or determine structural crack depth. " +
                "This report must be validated by a qualified Permanent Way Engineer (P-Way) or Railway Safety Inspector before any operational or maintenance certification is issued.", textFont);
        disclaimer.setSpacingBefore(8f);
        disclaimer.setSpacingAfter(14f);
        doc.add(disclaimer);

        // Sign-off Table
        PdfPTable signTable = new PdfPTable(3);
        signTable.setWidthPercentage(100);
        signTable.setWidths(new float[]{33f, 33f, 34f});

        addSignBlock(signTable, "Inspected By (AI Engine)", "TRACKVISION GUARD\nYOLO11-RailGuard-v3.0", titleFont, textFont);
        addSignBlock(signTable, "Field Inspector / Operator", defaultVal(insp.getUser() != null ? insp.getUser().getFullName() : null, "Field Inspector"), titleFont, textFont);
        addSignBlock(signTable, "Authorized P-Way Engineer", "Signature & Stamp:\n_______________________", titleFont, textFont);

        doc.add(signTable);
    }

    public byte[] generateHighRiskSafetySummaryReport(List<Inspection> highRiskInspections) {
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent());
            document.open();

            // 1. Header
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{70f, 30f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPaddingBottom(10f);

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, ALERT_RED);
            Paragraph brandPara = new Paragraph("TRACKVISION GUARD", brandFont);
            leftCell.addElement(brandPara);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_MUTED);
            Paragraph subPara = new Paragraph("Railway Track Defect Detection and Safety Monitoring System\nHIGH-RISK CRITICAL SAFETY AUDIT BRIEFING", subFont);
            leftCell.addElement(subPara);
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setPaddingBottom(10f);

            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, DARK_HEADER);
            Paragraph docTitle = new Paragraph("EXECUTIVE SAFETY AUDIT", metaFont);
            docTitle.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(docTitle);

            Font metaVal = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.DARK_GRAY);
            Paragraph dateP = new Paragraph("Audit Date: " + LocalDateTime.now().format(DATE_FMT), metaVal);
            dateP.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(dateP);
            headerTable.addCell(rightCell);
            document.add(headerTable);

            // Divider
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(ALERT_RED);
            lineCell.setFixedHeight(2.5f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            divider.addCell(lineCell);
            document.add(divider);

            document.add(new Paragraph(" "));

            // Overview Section
            Font secFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, DARK_HEADER);
            Paragraph overviewHeader = new Paragraph("1. High-Risk Track Anomaly Executive Summary", secFont);
            overviewHeader.setSpacingAfter(6f);
            document.add(overviewHeader);

            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.DARK_GRAY);
            Paragraph desc = new Paragraph("This document compiles all track inspections categorized as CRITICAL or HIGH risk across active rail divisions. Immediate field verification and ultrasonic flaw testing are required for all listed chainages.", bodyFont);
            desc.setSpacingAfter(10f);
            document.add(desc);

            // Summary Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{16f, 18f, 20f, 18f, 14f, 14f});

            Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
            String[] headers = {"Inspection #", "Zone / Section", "Location", "Coordinates", "Defects", "Risk Level"};
            for (String h : headers) {
                PdfPCell th = new PdfPCell(new Phrase(h, thFont));
                th.setBackgroundColor(DARK_HEADER);
                th.setBorderColor(DARK_HEADER);
                th.setPadding(6f);
                table.addCell(th);
            }

            if (highRiskInspections == null || highRiskInspections.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No critical or high-risk track sections currently identified in database records.", bodyFont));
                emptyCell.setColspan(6);
                emptyCell.setPadding(12f);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(emptyCell);
            } else {
                Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, DARK_HEADER);
                Font tdBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, DARK_HEADER);
                int rIdx = 0;
                for (Inspection insp : highRiskInspections) {
                    Color rowBg = (rIdx % 2 == 0) ? BG_LIGHT : Color.WHITE;
                    table.addCell(createCell(insp.getInspectionNumber(), tdBold, rowBg, Element.ALIGN_LEFT));
                    table.addCell(createCell(defaultVal(insp.getRailwayZone(), "-") + " / " + defaultVal(insp.getTrackSection(), "-"), tdFont, rowBg, Element.ALIGN_LEFT));
                    table.addCell(createCell(defaultVal(insp.getLocation(), "Unspecified"), tdFont, rowBg, Element.ALIGN_LEFT));
                    String gps = (insp.getLatitude() != null && insp.getLongitude() != null)
                            ? String.format("%.4f, %.4f", insp.getLatitude(), insp.getLongitude())
                            : "No GPS";
                    table.addCell(createCell(gps, tdFont, rowBg, Element.ALIGN_CENTER));

                    List<Detection> dList = detectionRepository.findByInspectionId(insp.getId());
                    int dCount = dList != null ? dList.size() : 0;
                    table.addCell(createCell(String.valueOf(dCount), tdFont, rowBg, Element.ALIGN_CENTER));

                    String rLvl = insp.getRiskLevel() != null ? insp.getRiskLevel().name() : "HIGH";
                    Color rColor = rLvl.equals("CRITICAL") ? ALERT_RED : ALERT_ORANGE;
                    Font rFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, rColor);
                    table.addCell(createCell(rLvl, rFont, rowBg, Element.ALIGN_CENTER));
                    rIdx++;
                }
            }
            document.add(table);

            document.add(new Paragraph(" "));

            // Statutory Disclaimer & Sign-off
            Font disclaimerFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXT_MUTED);
            Paragraph disc = new Paragraph("STATUTORY SAFETY NOTICE:\n" +
                    "This automated high-risk compilation is generated by TRACKVISION GUARD based on deep learning visual anomaly localization. " +
                    "Computer vision alerts indicate candidate structural discontinuities and do not constitute physical engineering clearance. " +
                    "An authorized Permanent Way (P-Way) official or Railway Safety Engineer must inspect and verify each corridor before operational speed authorizations are altered.", disclaimerFont);
            disc.setSpacingBefore(12f);
            disc.setSpacingAfter(14f);
            document.add(disc);

            // Sign-off
            PdfPTable signTable = new PdfPTable(3);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{33f, 33f, 34f});
            Font signTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, DARK_HEADER);
            addSignBlock(signTable, "Safety Intelligence System", "TRACKVISION GUARD\nYOLO11 Deep Learning Suite", signTitle, disclaimerFont);
            addSignBlock(signTable, "Divisional Safety Controller", "Reviewed & Flagged:\n_______________________", signTitle, disclaimerFont);
            addSignBlock(signTable, "Principal P-Way Safety Engineer", "Signature & Approval:\n_______________________", signTitle, disclaimerFont);
            document.add(signTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate High-Risk Safety Summary PDF: " + e.getMessage(), e);
        }
    }

    private void addSignBlock(PdfPTable table, String role, String detail, Font f1, Font f2) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.TOP);
        c.setBorderColor(BORDER_GRAY);
        c.setPaddingTop(8f);
        c.setPaddingBottom(4f);
        c.addElement(new Paragraph(role, f1));
        c.addElement(new Paragraph(detail, f2));
        table.addCell(c);
    }

    private String defaultVal(String v, String d) {
        return (v != null && !v.isBlank()) ? v.trim() : d;
    }

    // Page number & footer event
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Phrase p = new Phrase("TRACKVISION GUARD  |  Page " + writer.getPageNumber(), footerFont);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, p, document.right(), document.bottom() - 15, 0);
        }
    }
}
