package cn.kong.kb.ingestion.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 文档解析器：使用 Apache POI 逐工作表解析，
 * 将表格结构保留为 Markdown 表格，供 SheetChunker 按行窗口切分。
 */
@Component
public class ExcelDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(ExcelDocumentParser.class);

    @Override
    public List<Document> parse(MultipartFile file) {
        List<Document> documents = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                String markdown = sheetToMarkdown(sheet);

                if (markdown.isBlank()) continue;

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source_file", file.getOriginalFilename());
                metadata.put("file_type", "XLSX");
                metadata.put("sheet_name", sheetName);
                metadata.put("sheet_index", s);
                metadata.put("row_count", sheet.getLastRowNum() + 1);

                documents.add(new Document(markdown, metadata));
            }

            log.info("解析 Excel {}：共 {} 个工作表", file.getOriginalFilename(), documents.size());
        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败：" + e.getMessage(), e);
        }

        return documents;
    }

    /**
     * 将 Excel 工作表转换为 Markdown 表格格式。
     */
    private String sheetToMarkdown(Sheet sheet) {
        if (sheet.getLastRowNum() < 0) return "";

        StringBuilder sb = new StringBuilder();

        // 表头行（第一行）
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return "";

        sb.append("| ");
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            sb.append(getCellValue(cell)).append(" | ");
        }
        sb.setLength(sb.length() - 1); // 移除末尾多余的空格
        sb.append("\n");

        // 分隔行
        sb.append("| ");
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            sb.append("--- | ");
        }
        sb.setLength(sb.length() - 1);
        sb.append("\n");

        // 数据行
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            sb.append("| ");
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                sb.append(getCellValue(cell)).append(" | ");
            }
            sb.setLength(sb.length() - 1);
            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        String value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().replace("|", "\\|").replace("\n", " ");
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
        // 截断超长单元格内容，防止单个切片超出 Embedding token 限制
        if (value.length() > 100) {
            value = value.substring(0, 100) + "...";
        }
        return value;
    }
}
