package school.hei.vola.repository;

import static java.util.UUID.randomUUID;
import static school.hei.vola.model.Time.millisNow;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.JPaymentRepository;
import school.hei.vola.repository.jpa.JUserRepository;
import school.hei.vola.repository.jpa.mapper.JPaymentMapper;
import school.hei.vola.repository.jpa.model.JPayment;
import school.hei.vola.repository.jpa.model.JUser;

@Slf4j
@Repository
@AllArgsConstructor
public class PaymentRepository {
  private static final int FAILED_PAYMENT_ATTEMPT_COUNT = 6;

  private final JPaymentRepository jPaymentRepository;
  private final JPaymentMapper jPaymentMapper;
  private final JUserRepository jUserRepository;
  private final JApplicationRepository jApplicationRepository;

  public Payment createPayment(
      String apiKey, String payerEmail, PspType pspType, String pspPaymentId) {
    var jUserSaved = jUserRepository.findByEmail(payerEmail);
    if (jUserSaved == null) {
      var jUserToSave = new JUser();
      jUserToSave.setEmail(payerEmail);
      jUserToSave.setId(randomUUID().toString());
      jUserSaved = jUserRepository.save(jUserToSave);
    }

    var jApplication = jApplicationRepository.findByApiKey(apiKey).get();
    var toSaveJPayment =
        new JPayment(
            randomUUID().toString(),
            pspType,
            null,
            pspPaymentId,
            null,
            millisNow(),
            null,
            0,
            jUserSaved,
            jApplication);
    var savedJPayment = jPaymentRepository.save(toSaveJPayment);

    return jPaymentMapper.toDomain(savedJPayment);
  }

  public Payment findPaymentById(String id) {
    var jPayment = jPaymentRepository.findById(id).get();
    return jPaymentMapper.toDomain(jPayment);
  }

  public Payment save(Payment payment) {
    var jPayment = jPaymentMapper.toEntity(payment);
    return jPaymentMapper.toDomain(jPaymentRepository.save(jPayment));
  }

  public Optional<Payment> findPaymentByPspTypeAndPspPaymentId(
      PspType pspType, String pspPaymentId) {
    return jPaymentRepository
        .findByPspTypeAndPspPaymentId(pspType, pspPaymentId)
        .map(jPaymentMapper::toDomain);
  }

  public Optional<Payment> findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
      String payerEmail, PspType pspType, String pspPaymentId) {
    return jPaymentRepository
        .findPaymentByPayerEmailAndPspTypeAndPspPaymentId(payerEmail, pspType, pspPaymentId)
        .map(jPaymentMapper::toDomain);
  }

  public List<Payment> findPaymentsByPaymentInfos(List<PaymentInfo> paymentInfos) {
    log.info("[PAYMENT_SEARCH] Searching for {} payment(s)", paymentInfos.size());

    return paymentInfos.stream().map(this::findOrCreateFailedPayment).distinct().toList();
  }

  private Payment findOrCreateFailedPayment(PaymentInfo info) {
    log.debug(
        "[PAYMENT_SEARCH] Searching payment: email={}, pspType={}, pspPaymentId={}",
        info.payerEmail(),
        info.pspType(),
        info.pspPaymentId());

    var jPaymentOptional =
        jPaymentRepository.findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
            info.payerEmail(), info.pspType(), info.pspPaymentId());

    return jPaymentOptional
        .map(jPayment -> mapToFoundPayment(jPayment, info))
        .orElseGet(() -> createFailedPlaceholderPayment(info));
  }

  private Payment mapToFoundPayment(JPayment jPayment, PaymentInfo info) {
    var payment = jPaymentMapper.toDomain(jPayment);

    log.info(
        "[PAYMENT_SEARCH] ✓ Payment FOUND: id={}, email={}, pspType={}, pspPaymentId={}, status={}",
        payment.id(),
        info.payerEmail(),
        info.pspType(),
        info.pspPaymentId(),
        payment.getVerificationStatus());

    return payment;
  }

  private Payment createFailedPlaceholderPayment(PaymentInfo info) {
    log.warn(
        "[PAYMENT_SEARCH] ✗ Payment NOT FOUND: email={}, pspType={}, pspPaymentId={} - Creating"
            + " FAILED placeholder",
        info.payerEmail(),
        info.pspType(),
        info.pspPaymentId());

    return Payment.builder()
        .pspPayment(new PspPayment(info.pspType(), info.pspPaymentId(), null, null))
        .verificationAttemptNb(FAILED_PAYMENT_ATTEMPT_COUNT)
        .payer(new User(info.payerEmail()))
        .build();
  }
}
