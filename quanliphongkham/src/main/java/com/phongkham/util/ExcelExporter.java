package com.phongkham.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public final class ExcelExporter {
    private ExcelExporter() {}

    public static void export(File file, Map<String, List<List<Object>>> sheets) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            for (Map.Entry<String, List<List<Object>>> entry : sheets.entrySet()) {
                Sheet sheet = workbook.createSheet(entry.getKey());
                sheet.setDisplayGridlines(false);
                sheet.setFitToPage(true);
                sheet.getPrintSetup().setLandscape(true);
                sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
                sheet.getPrintSetup().setFitWidth((short) 1);
                sheet.getPrintSetup().setFitHeight((short) 0);
                sheet.setAutobreaks(true);
                sheet.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
                sheet.createFreezePane(0, 1);

                int rowIndex = 0;
                int columnCount = 0;
                for (List<Object> values : entry.getValue()) {
                    Row row = sheet.createRow(rowIndex);
                    columnCount = Math.max(columnCount, values.size());
                    for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                        Cell cell = row.createCell(columnIndex);
                        Object value = values.get(columnIndex);
                        if (rowIndex == 0) {
                            cell.setCellStyle(headerStyle);
                        }
                        if (value instanceof BigDecimal decimal) {
                            cell.setCellValue(decimal.doubleValue());
                        } else if (value instanceof Number number) {
                            cell.setCellValue(number.doubleValue());
                        } else if (value instanceof Boolean bool) {
                            cell.setCellValue(bool);
                        } else if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                    if (rowIndex == 0) {
                        row.setHeightInPoints(30);
                    }
                    rowIndex++;
                }
                if (columnCount > 0) {
                    sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, columnCount - 1));
                    for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                        sheet.autoSizeColumn(columnIndex);
                        sheet.setColumnWidth(columnIndex, Math.min(sheet.getColumnWidth(columnIndex) + 512, 12000));
                    }
                    sheet.getPrintSetup().setFitWidth((short) 1);
                }
            }

            try (OutputStream output = Files.newOutputStream(file.toPath())) {
                workbook.write(output);
            }
        }
    }
}
