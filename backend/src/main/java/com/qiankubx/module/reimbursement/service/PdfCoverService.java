package com.qiankubx.module.reimbursement.service;

import com.aliyun.oss.OSS;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.qiankubx.common.config.OssConfig;
import com.qiankubx.module.expense.entity.Expense;
import com.qiankubx.module.reimbursement.entity.Reimbursement;
import com.qiankubx.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfCoverService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    private static final DeviceRgb HEADER_BG = new DeviceRgb(66, 133, 244);
    private static final DeviceRgb LIGHT_GRAY_BG = new DeviceRgb(245, 245, 245);

    public String generateCoverPdf(Reimbursement reimbursement, User user, List<Expense> expenses,
                                   Map<Long, String> categoryMap) {
        try {
            byte[] pdfBytes = buildPdf(reimbursement, user, expenses, categoryMap != null ? categoryMap : Collections.emptyMap());
            String objectKey = ossConfig.getDirs().getReimbursement()
                    + reimbursement.getReimburseNo() + "_cover.pdf";

            ossClient.putObject(ossConfig.getBucketName(), objectKey,
                    new ByteArrayInputStream(pdfBytes));

            String url = buildOssUrl(objectKey);
            log.info("报销单封面PDF已上传: {}", url);
            return url;
        } catch (Exception e) {
            log.error("生成报销单封面PDF失败", e);
            throw new RuntimeException("生成报销单封面PDF失败: " + e.getMessage(), e);
        }
    }

    public String generateCoverPdf(Reimbursement reimbursement, User user, List<Expense> expenses) {
        return generateCoverPdf(reimbursement, user, expenses, Collections.emptyMap());
    }

    private byte[] buildPdf(Reimbursement reimbursement, User user, List<Expense> expenses,
                            Map<Long, String> categoryMap) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        PdfFont font = createChineseFont();

        addTitle(document, font);
        addReimbursementInfo(document, font, reimbursement, user);
        addExpenseTable(document, font, expenses, categoryMap);
        addTotalAmount(document, font, reimbursement);
        addAttachmentList(document, font, expenses);

        if (reimbursement.getRemark() != null && !reimbursement.getRemark().isEmpty()) {
            document.add(new Paragraph("备注：" + reimbursement.getRemark())
                    .setFont(font).setFontSize(10).setMarginTop(15));
        }

        addSignatureArea(document, font);

        if (user.getMemberStatus() == null || user.getMemberStatus() == 0) {
            addWatermark(pdfDoc, font);
        }

        document.close();
        return baos.toByteArray();
    }

    private PdfFont createChineseFont() {
        try {
            return PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
        } catch (Exception e1) {
            log.warn("无法加载STSongStd-Light字体，尝试系统字体");
            String[] systemFonts = {
                    "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0",
                    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc,0",
                    "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",
                    "C:/Windows/Fonts/simsun.ttc,0"
            };
            for (String fontPath : systemFonts) {
                try {
                    return PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
                } catch (Exception ignored) {
                }
            }
            log.warn("无法加载系统中文字体，使用默认字体");
            try {
                return PdfFontFactory.createFont();
            } catch (Exception e) {
                throw new RuntimeException("无法创建字体", e);
            }
        }
    }

    private void addTitle(Document document, PdfFont font) {
        Paragraph title = new Paragraph("费用报销单")
                .setFont(font)
                .setFontSize(22)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);
    }

    private void addReimbursementInfo(Document document, PdfFont font,
                                      Reimbursement reimbursement, User user) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        addInfoRow(infoTable, font, "报销单号", reimbursement.getReimburseNo());
        addInfoRow(infoTable, font, "报销标题", reimbursement.getTitle());
        addInfoRow(infoTable, font, "报销人", user.getNickname() != null ? user.getNickname() : "-");
        addInfoRow(infoTable, font, "所属部门", user.getDepartment() != null ? user.getDepartment() : "-");
        addInfoRow(infoTable, font, "所属公司", user.getCompany() != null ? user.getCompany() : "-");
        addInfoRow(infoTable, font, "申请日期",
                reimbursement.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        document.add(infoTable);
    }

    private void addInfoRow(Table table, PdfFont font, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(font).setFontSize(10))
                .setBackgroundColor(LIGHT_GRAY_BG)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(5));
        table.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "").setFont(font).setFontSize(10))
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(5));
    }

    private void addExpenseTable(Document document, PdfFont font, List<Expense> expenses,
                                Map<Long, String> categoryMap) {
        Paragraph subtitle = new Paragraph("费用明细")
                .setFont(font)
                .setFontSize(14)
                .setBold()
                .setMarginTop(10)
                .setMarginBottom(8);
        document.add(subtitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 1.5f, 2f, 1.5f, 1f}))
                .useAllAvailableWidth()
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        String[] headers = {"序号", "费用类别", "发票号码", "金额(元)", "备注"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setFont(font).setFontSize(9).setBold())
                    .setBackgroundColor(HEADER_BG)
                    .setFontColor(ColorConstants.WHITE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(new SolidBorder(ColorConstants.WHITE, 0.5f))
                    .setPadding(6));
        }

        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            DeviceRgb rowBg = (i % 2 == 0) ? new DeviceRgb(255, 255, 255) : LIGHT_GRAY_BG;

            String categoryName = resolveCategoryName(expense, categoryMap);

            table.addCell(createCell(String.valueOf(i + 1), font, rowBg, TextAlignment.CENTER));
            table.addCell(createCell(categoryName, font, rowBg, TextAlignment.LEFT));
            table.addCell(createCell(
                    expense.getInvoiceNo() != null ? expense.getInvoiceNo() : "-",
                    font, rowBg, TextAlignment.LEFT));
            table.addCell(createCell(
                    expense.getAmount() != null ? expense.getAmount().toPlainString() : "0",
                    font, rowBg, TextAlignment.RIGHT));
            table.addCell(createCell(
                    expense.getDescription() != null ? expense.getDescription() : "",
                    font, rowBg, TextAlignment.LEFT));
        }

        document.add(table);
    }

    private String resolveCategoryName(Expense expense, Map<Long, String> categoryMap) {
        if (expense.getCategoryId() != null && categoryMap.containsKey(expense.getCategoryId())) {
            return categoryMap.get(expense.getCategoryId());
        }
        if (expense.getInvoiceType() != null && !expense.getInvoiceType().isBlank()) {
            return expense.getInvoiceType();
        }
        return "其他";
    }

    private Cell createCell(String text, PdfFont font, DeviceRgb bgColor, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setBackgroundColor(bgColor)
                .setTextAlignment(alignment)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f))
                .setPadding(4);
    }

    private void addTotalAmount(Document document, PdfFont font, Reimbursement reimbursement) {
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{5f, 1.5f}))
                .useAllAvailableWidth()
                .setMarginTop(5);

        totalTable.addCell(new Cell()
                .add(new Paragraph("合计金额").setFont(font).setFontSize(11).setBold())
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
                .setPadding(5));

        totalTable.addCell(new Cell()
                .add(new Paragraph("¥ " + reimbursement.getTotalAmount().toPlainString())
                        .setFont(font).setFontSize(11).setBold()
                        .setFontColor(new DeviceRgb(220, 53, 69)))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
                .setPadding(5));

        document.add(totalTable);
    }

    private void addAttachmentList(Document document, PdfFont font, List<Expense> expenses) {
        List<Expense> withFiles = expenses.stream()
                .filter(e -> e.getFileUrl() != null && !e.getFileUrl().isBlank())
                .toList();

        if (withFiles.isEmpty()) {
            return;
        }

        Paragraph subtitle = new Paragraph("附件清单")
                .setFont(font)
                .setFontSize(14)
                .setBold()
                .setMarginTop(15)
                .setMarginBottom(8);
        document.add(subtitle);

        for (int i = 0; i < withFiles.size(); i++) {
            Expense expense = withFiles.get(i);
            String category = expense.getInvoiceType() != null ? expense.getInvoiceType() : "发票";
            String invoiceNo = expense.getInvoiceNo() != null ? expense.getInvoiceNo() : "无编号";
            String line = (i + 1) + ". " + category + " - " + invoiceNo
                    + " (" + expense.getAmount().toPlainString() + "元)";
            document.add(new Paragraph(line).setFont(font).setFontSize(9).setMarginBottom(2));
        }
    }

    private void addSignatureArea(Document document, PdfFont font) {
        document.add(new Paragraph("").setMarginTop(30));

        Table signTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginTop(20);

        String[] labels = {"报销人签字：", "部门主管签字：", "财务审批签字："};
        for (String label : labels) {
            signTable.addCell(new Cell()
                    .add(new Paragraph(label + "___________").setFont(font).setFontSize(10))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(10));
        }

        String[] dateLine = {"日期：___________", "日期：___________", "日期：___________"};
        for (String d : dateLine) {
            signTable.addCell(new Cell()
                    .add(new Paragraph(d).setFont(font).setFontSize(10))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(10));
        }

        document.add(signTable);

        document.add(new Paragraph("生成时间：" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setFont(font)
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(20));
    }

    private void addWatermark(PdfDocument pdfDoc, PdfFont font) {
        PdfExtGState gs = new PdfExtGState().setFillOpacity(0.15f);
        DeviceRgb color = new DeviceRgb(180, 180, 180);
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            PdfPage page = pdfDoc.getPage(i);
            PdfCanvas canvas = new PdfCanvas(page);
            canvas.saveState();
            canvas.setExtGState(gs);
            canvas.setFillColor(color);
            canvas.beginText();
            canvas.setFontAndSize(font, 40);
            float pageWidth = page.getPageSize().getWidth();
            float pageHeight = page.getPageSize().getHeight();
            float x = pageWidth / 2 - 80;
            float y = pageHeight / 2;
            canvas.setTextMatrix(
                    (float) Math.cos(Math.toRadians(45)), (float) Math.sin(Math.toRadians(45)),
                    (float) -Math.sin(Math.toRadians(45)), (float) Math.cos(Math.toRadians(45)),
                    x, y);
            canvas.showText("\u94B1\u9177\u62A5\u9500");
            canvas.endText();
            canvas.restoreState();
        }
    }

    private String buildOssUrl(String objectKey) {
        if (ossConfig.getCdnDomain() != null && !ossConfig.getCdnDomain().isBlank()) {
            String domain = ossConfig.getCdnDomain();
            if (!domain.startsWith("http")) {
                domain = "https://" + domain;
            }
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            return domain + "/" + objectKey;
        }
        return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectKey;
    }
}
