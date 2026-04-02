package school.hei.vola.service;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final EventProducer eventProducer;
  private final OrangePaymentRepository orangePaymentRepository;

  @Transactional
  public Payment createPayment(
      String apiKey, String payerEmail, PspType pspType, String pspPaymentId) {
    var payment = paymentRepository.createPayment(apiKey, payerEmail, pspType, pspPaymentId);

    eventProducer.accept(List.of(new PaymentVerificationRequested(payment)));
    log.info("PaymentVerificationRequested event sent for payment={}", payment);

    return payment;
  }

  public List<Payment> createPayments(String apiKey, List<PaymentInfo> paymentInfos) {
    var payments = paymentRepository.createPayments(apiKey, paymentInfos);
    if (payments.isEmpty()) {
      return List.of();
    }

    var paymentRequests = payments.stream().map(PaymentVerificationRequested::new).toList();
    eventProducer.accept(paymentRequests);
    log.info("PaymentVerificationRequested event sent for {} payments", payments.size());

    return payments;
  }

  public Optional<Payment> findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
      String payerEmail, PspType pspType, String pspPaymentId) {
    return paymentRepository.findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
        payerEmail, pspType, pspPaymentId);
  }

  public List<Payment> findPaymentsByPaymentInfos(String apiKey, List<PaymentInfo> paymentInfos) {
    var foundPayments = paymentRepository.findPaymentsByPaymentInfos(paymentInfos);
    var foundPaymentInfos =
        new HashSet<>(
            foundPayments.stream()
                .map(
                    p ->
                        new PaymentInfo(
                            p.payer().email(), p.pspPayment().pspType(), p.pspPayment().id()))
                .toList());
    var missingPaymentInfos =
        paymentInfos.stream().filter(info -> !foundPaymentInfos.contains(info)).toList();
    if (!missingPaymentInfos.isEmpty()) {
      createPayments(apiKey, missingPaymentInfos);
    }
    return foundPayments;
  }

  public int saveTransactionFromExcel(MultipartFile excel) throws IOException {
    ArrayList<OrangeTransaction> orangeTransactions = new ArrayList<>();

    try (Workbook workbook = WorkbookFactory.create(excel.getInputStream())) {
      Sheet sheet = workbook.getSheetAt(0);
      for (Row row : sheet) {
        if (row.getRowNum() == 0) continue;
        int number = Integer.parseInt(row.getCell(0).getStringCellValue());
        String date = row.getCell(1).getStringCellValue();
        String time = row.getCell(2).getStringCellValue();
        String ref = row.getCell(3).getStringCellValue();
        String status = row.getCell(6).getStringCellValue();
        String clientNumber = row.getCell(11).getStringCellValue();
        int amount = Integer.parseInt(row.getCell(14).getStringCellValue());

        OrangeTransaction orangeTransaction =
            new OrangeTransaction(number, date, time, ref, status, clientNumber, amount);
        orangeTransactions.add(orangeTransaction);
      }

      orangePaymentRepository.saveAll(orangeTransactions);
    } catch (IOException e) {
      throw new IOException("failed to load the xls file");
    }
    return orangeTransactions.size();
  }
}
