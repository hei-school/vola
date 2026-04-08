package school.hei.vola.service.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;
import school.hei.vola.model.psp.orange.OrangeTransaction;

@AllArgsConstructor
public class ExcelParser<T> {
  private final Class<T> clazz;

  public List<OrangeTransaction> parseToOrangeTransaction(MultipartFile excel) throws IOException {
    ArrayList<OrangeTransaction> orangeTransactions = new ArrayList<>();

    try (Workbook workbook = WorkbookFactory.create(excel.getInputStream())) {
      var sheet = workbook.getSheetAt(0);
      for (Row row : sheet) {
        var firstCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (firstCell == null || firstCell.getCellType() != CellType.NUMERIC) continue;

        var number = (int) row.getCell(0).getNumericCellValue();
        String date = getCellAsString(row, 1);
        String time = getCellAsString(row, 2);
        String ref = getCellAsString(row, 3);
        String status = getCellAsString(row, 6);
        String clientNumber = getCellAsString(row, 11);

        Cell creditCell = row.getCell(14, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (creditCell == null || creditCell.getCellType() != CellType.NUMERIC) continue;
        int amount = (int) creditCell.getNumericCellValue();

        orangeTransactions.add(
            new OrangeTransaction(number, date, time, ref, status, clientNumber, amount));
      }

      return orangeTransactions;
    } catch (IOException e) {
      throw new IOException("Failed to load the xls file", e);
    }
  }

  private String getCellAsString(Row row, int colIndex) {
    var cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      default -> "";
    };
  }
}
