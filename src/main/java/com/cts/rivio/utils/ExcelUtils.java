package com.cts.rivio.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

/**
 * ExcelUtils – read/write Excel (.xlsx) test data using Apache POI.
 *
 * Apache POI key classes:
 *   Workbook  → the whole .xlsx file
 *   Sheet     → one tab/worksheet inside the workbook
 *   Row       → a horizontal row on a sheet
 *   Cell      → a single cell (column + row intersection)
 *
 * DataFormatter handles mixed cell types (numeric, string, boolean)
 * and returns them all as Strings, which is what DataProviders need.
 */
public class ExcelUtils {

    private ExcelUtils() {}

    // ── Read all data from a sheet as a 2D String array ───────────────────────

    /**
     * Reads every row (including the header row=0) from the given sheet.
     * Returns data[row][col] as String.
     *
     * @param filePath  path to the .xlsx file
     * @param sheetName name of the worksheet tab
     */
    public static String[][] readData(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet not found: " + sheetName);
            }

            int rowCount  = sheet.getPhysicalNumberOfRows();
            int colCount  = sheet.getRow(0).getPhysicalNumberOfCells();
            DataFormatter fmt = new DataFormatter();

            String[][] data = new String[rowCount][colCount];
            for (int r = 0; r < rowCount; r++) {
                Row row = sheet.getRow(r);
                for (int c = 0; c < colCount; c++) {
                    Cell cell = (row != null) ? row.getCell(c) : null;
                    data[r][c] = (cell != null) ? fmt.formatCellValue(cell) : "";
                }
            }
            return data;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }
    }

    /**
     * Reads all rows EXCEPT the header row (row 0).
     * Returns a 2D array suitable for a TestNG @DataProvider.
     */
    public static Object[][] readDataExcludingHeader(String filePath, String sheetName) {
        String[][] raw = readData(filePath, sheetName);
        // Skip row 0 (header)
        Object[][] data = new Object[raw.length - 1][raw[0].length];
        for (int r = 1; r < raw.length; r++) {
            data[r - 1] = raw[r];
        }
        return data;
    }

    // ── Read a single cell value ───────────────────────────────────────────────

    public static String getCellData(String filePath, String sheetName, int rowNum, int colNum) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            Row   row   = sheet.getRow(rowNum);
            Cell  cell  = row.getCell(colNum);
            return new DataFormatter().formatCellValue(cell);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read cell (" + rowNum + "," + colNum + ")", e);
        }
    }

    // ── Write a value back to Excel (e.g., test result) ───────────────────────

    /**
     * Writes a value into a specific cell and saves the file.
     * Used to write test results (PASS/FAIL) back into the Excel sheet.
     */
    public static void setCellData(String filePath, String sheetName,
                                   int rowNum, int colNum, String value) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            Row   row   = sheet.getRow(rowNum);
            if (row == null) row = sheet.createRow(rowNum);

            Cell cell = row.getCell(colNum);
            if (cell == null) cell = row.createCell(colNum);
            cell.setCellValue(value);

            // Write back to the same file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write cell (" + rowNum + "," + colNum + ")", e);
        }
    }

    // ── Read rows as List<Map> – column header as key ─────────────────────────

    /**
     * Returns each data row as a Map<columnHeader, cellValue>.
     * Convenient when column order might change.
     */
    public static List<Map<String, String>> readDataAsMapList(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            DataFormatter fmt = new DataFormatter();

            // Header row
            Row headerRow = sheet.getRow(0);
            int colCount  = headerRow.getPhysicalNumberOfCells();
            String[] headers = new String[colCount];
            for (int c = 0; c < colCount; c++) {
                headers[c] = fmt.formatCellValue(headerRow.getCell(c));
            }

            List<Map<String, String>> result = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int c = 0; c < colCount; c++) {
                    Cell cell = row.getCell(c);
                    rowMap.put(headers[c], cell != null ? fmt.formatCellValue(cell) : "");
                }
                result.add(rowMap);
            }
            return result;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel as map: " + filePath, e);
        }
    }

    // ── Count rows (excluding header) ─────────────────────────────────────────

    public static int getRowCount(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return sheet.getPhysicalNumberOfRows() - 1; // minus header
        } catch (IOException e) {
            throw new RuntimeException("Failed to count rows", e);
        }
    }
}
