package school.hei.vola.unit.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import school.hei.vola.service.utils.ExcelParser;

public class ExcelParserTest {
  ExcelParser parser = new ExcelParser();

  private static final String VALID_REF = "MP260407.0815.D41814";
  private static final String VALID_CLIENT = "0312345678";
  private static final String VALID_DATE = "07/04/2026";
  private static final String VALID_TIME = "08:15:00";

  @Test
  void getCellAsString_nullCell_returnsEmptyString() {
    Workbook wb = new HSSFWorkbook();
    Row row = wb.createSheet().createRow(0);
    assertEquals("", parser.getCellAsString(row, 0));
  }

  @Test
  void getCellAsString_stringCell_returnsValue() {
    Workbook wb = new HSSFWorkbook();
    Row row = wb.createSheet().createRow(0);
    row.createCell(0).setCellValue("hello");
    assertEquals("hello", parser.getCellAsString(row, 0));
  }

  @Test
  void getCellAsString_numericInteger_returnsWithoutDecimal() {
    Workbook wb = new HSSFWorkbook();
    Row row = wb.createSheet().createRow(0);
    row.createCell(0).setCellValue(42.0);
    assertEquals("42", parser.getCellAsString(row, 0));
  }

  @Test
  void getCellAsString_numericWithDecimal_shouldKeepDecimals() {
    Workbook wb = new HSSFWorkbook();
    Sheet sheet = wb.createSheet();
    Row row = sheet.createRow(0);
    row.createCell(0).setCellValue(12.5);
    assertEquals("12.5", parser.getCellAsString(row, 0));
  }

  @Test
  void getCellAsString_booleanCell_returnsStringBoolean() {
    Workbook wb = new HSSFWorkbook();
    Row row = wb.createSheet().createRow(0);
    row.createCell(0).setCellValue(true);
    assertEquals("true", parser.getCellAsString(row, 0));
  }

  @Test
  void parse_validRow_goesToSucceeded() throws IOException {
    File file =
        createXls(
            new Object[][] {
              {
                1.0,
                VALID_DATE,
                VALID_TIME,
                VALID_REF,
                "",
                "",
                "Succès",
                "",
                "",
                "",
                "",
                VALID_CLIENT,
                "",
                "",
                5000.0
              }
            });

    var result = parser.parseToOrangeTransaction(file);

    assertEquals(1, result.validTransactions().size());
    assertEquals(0, result.invalidTransactions().size());

    var tx = result.validTransactions().get(0);
    assertEquals(1, tx.getNumber());
    assertEquals(VALID_REF, tx.getRef());
    assertEquals(VALID_CLIENT, tx.getClientNumber());
    assertEquals(5000, tx.getAmount());
  }

  @Test
  void parse_invalidRef_goesToFailed() throws IOException {
    File file =
        createXls(
            new Object[][] {
              {
                1.0,
                VALID_DATE,
                VALID_TIME,
                "INVALID_REF",
                "",
                "",
                "Succès",
                "",
                "",
                "",
                "",
                VALID_CLIENT,
                "",
                "",
                5000.0
              }
            });

    var result = parser.parseToOrangeTransaction(file);

    assertEquals(0, result.validTransactions().size());
    assertEquals(1, result.invalidTransactions().size());
  }

  @Test
  void parse_invalidClientNumber_goesToFailed() throws IOException {
    File file =
        createXls(
            new Object[][] {
              {
                1.0,
                VALID_DATE,
                VALID_TIME,
                VALID_REF,
                "",
                "",
                "Succès",
                "",
                "",
                "",
                "",
                "0000000000",
                "",
                "",
                5000.0
              }
            });

    var result = parser.parseToOrangeTransaction(file);

    assertEquals(0, result.validTransactions().size());
    assertEquals(1, result.invalidTransactions().size());
  }

  @Test
  void parse_nullCreditCell_rowIsSkipped() throws IOException {
    File file =
        createXls(
            new Object[][] {
              {
                1.0,
                VALID_DATE,
                VALID_TIME,
                VALID_REF,
                "",
                "",
                "Succès",
                "",
                "",
                "",
                "",
                VALID_CLIENT,
                "",
                "",
                null
              }
            });

    var result = parser.parseToOrangeTransaction(file);

    assertEquals(0, result.validTransactions().size());
    assertEquals(0, result.invalidTransactions().size());
  }

  @Test
  void parse_firstCellNotNumeric_rowIsSkipped() throws IOException {
    Workbook wb = new HSSFWorkbook();
    Row row = wb.createSheet().createRow(0);
    row.createCell(0).setCellValue("header");
    File tmp = File.createTempFile("test-orange-header", ".xls");
    try (var out = new FileOutputStream(tmp)) {
      wb.write(out);
    }

    var result = parser.parseToOrangeTransaction(tmp);

    assertEquals(0, result.validTransactions().size());
    assertEquals(0, result.invalidTransactions().size());
  }

  @Test
  void parse_multipleRows_correctlyDistributed() throws IOException {
    File file =
        createXls(
            new Object[][] {
              {
                1.0,
                VALID_DATE,
                VALID_TIME,
                VALID_REF,
                "",
                "",
                "Succès",
                "",
                "",
                "",
                "",
                VALID_CLIENT,
                "",
                "",
                5000.0
              },
              {
                2.0,
                VALID_DATE,
                VALID_TIME,
                "BAD_REF",
                "",
                "",
                "Succès",
                "",
                "",
                "",
                "",
                VALID_CLIENT,
                "",
                "",
                3000.0
              },
              {
                3.0,
                VALID_DATE,
                VALID_TIME,
                VALID_REF,
                "",
                "",
                "Succès",
                "",
                "",
                "",
                "",
                "0000000000",
                "",
                "",
                1000.0
              },
            });

    var result = parser.parseToOrangeTransaction(file);

    assertEquals(1, result.validTransactions().size());
    assertEquals(2, result.invalidTransactions().size());
  }

  @Test
  void parse_emptySheet_returnsEmptyLists() throws IOException {
    File file = createXls(new Object[][] {});

    var result = parser.parseToOrangeTransaction(file);

    assertEquals(0, result.validTransactions().size());
    assertEquals(0, result.invalidTransactions().size());
  }

  private File createXls(Object[][] rows) throws IOException {
    Workbook wb = new HSSFWorkbook();
    Sheet sheet = wb.createSheet();
    for (int r = 0; r < rows.length; r++) {
      Row row = sheet.createRow(r);
      for (int c = 0; c < rows[r].length; c++) {
        if (rows[r][c] == null) continue;
        Cell cell = row.createCell(c);
        if (rows[r][c] instanceof Double d) cell.setCellValue(d);
        else if (rows[r][c] instanceof String s) cell.setCellValue(s);
      }
    }
    File tmp = File.createTempFile("test-orange", ".xls");
    try (var out = new FileOutputStream(tmp)) {
      wb.write(out);
    }
    return tmp;
  }
}
