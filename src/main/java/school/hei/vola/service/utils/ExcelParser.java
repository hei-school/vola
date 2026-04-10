package school.hei.vola.service.utils;

import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
import static org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import school.hei.vola.model.ImportedTransactionDetails;
import school.hei.vola.model.psp.orange.OrangeTransaction;

@Component
@AllArgsConstructor
public class ExcelParser {

  public ImportedTransactionDetails parseToOrangeTransaction(File excel) throws IOException {
    var failed = new ArrayList<OrangeTransaction>();
    var succed = new ArrayList<OrangeTransaction>();

    try (var workbook = WorkbookFactory.create(excel)) {
      var sheet = workbook.getSheetAt(0);
      for (Row row : sheet) {
        var firstCell = row.getCell(0, RETURN_BLANK_AS_NULL);
        if (firstCell == null || firstCell.getCellType() != NUMERIC) continue;

        var number = (int) row.getCell(0).getNumericCellValue();
        var date = getCellAsString(row, 1);
        var time = getCellAsString(row, 2);
        var ref = getCellAsString(row, 3);
        var status = getCellAsString(row, 6);
        var clientNumber = getCellAsString(row, 11);

        var creditCell = row.getCell(14, RETURN_BLANK_AS_NULL);
        if (creditCell == null || creditCell.getCellType() != NUMERIC) continue;
        var amount = (int) creditCell.getNumericCellValue();
        var parsed = new OrangeTransaction(number, date, time, ref, status, clientNumber, amount);

        if (!parsed.validateRef(ref) || !parsed.validateClientNumber(clientNumber)) {
          failed.add(parsed);
        } else {
          succed.add(parsed);
        }
      }

      return new ImportedTransactionDetails(failed, succed);
    } catch (IOException e) {
      throw new IOException("Failed to load the xls file", e);
    }
  }

  public String getCellAsString(Row row, int colIndex) {
    var cell = row.getCell(colIndex, RETURN_BLANK_AS_NULL);
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> {
        double val = cell.getNumericCellValue();
        yield (val == Math.floor(val))
            ? String.valueOf((long) val) // "12" si pas de décimales
            : String.valueOf(val); // "12.5" si décimales
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      default -> "";
    };
  }
}
