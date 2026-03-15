package com.aidb.service;

import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;

@Service
public class ExportService {

    public byte[] exportToCSV(List<Map<String, Object>> data, List<String> columns) throws IOException {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(columns.toArray(new String[0]));
            for (Map<String, Object> row : data) {
                String[] line = columns.stream()
                    .map(col -> row.get(col) == null ? "" : row.get(col).toString())
                    .toArray(String[]::new);
                writer.writeNext(line);
            }
        }
        return sw.toString().getBytes();
    }

    public byte[] exportToExcel(List<Map<String, Object>> data, List<String> columns) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Query Results");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            CellStyle altStyle = workbook.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);
                for (int j = 0; j < columns.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object val = rowData.get(columns.get(j));
                    if (val == null) {
                        cell.setCellValue("");
                    } else if (val instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(val.toString());
                    }
                    if (i % 2 == 1) cell.setCellStyle(altStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.size(); i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
