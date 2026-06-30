package school.hei.vola.repository;

import static java.util.UUID.randomUUID;
import static school.hei.vola.model.Time.millisNow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.JPaymentFilterRepository;
import school.hei.vola.repository.jpa.JPaymentRepository;
import school.hei.vola.repository.jpa.JPaymentRepositoryCustom;
import school.hei.vola.repository.jpa.JUserRepository;
import school.hei.vola.repository.jpa.mapper.JPaymentMapper;
import school.hei.vola.repository.jpa.model.JPayment;
import school.hei.vola.repository.jpa.model.JUser;

@Slf4j
@Repository
@AllArgsConstructor
public class PaymentRepository {

  private final JPaymentRepositoryCustom jPaymentRepositoryCustom;
  private final JPaymentRepository jPaymentRepository;
  private final JPaymentFilterRepository jPaymentFilterRepository;
  private final JPaymentMapper jPaymentMapper;
  private final JUserRepository jUserRepository;
  private final JApplicationRepository jApplicationRepository;

  public Payment createPayment(
      String apiKey, String payerEmail, PspType pspType, String pspPaymentId, String scope) {
    var existing = jPaymentRepository.findByPspTypeAndPspPaymentId(pspType, pspPaymentId);
    if (existing.isPresent()) {
      throw new IllegalArgumentException(
          "Payment with pspType="
              + pspType
              + " and pspPaymentId="
              + pspPaymentId
              + " already exists, owned by "
              + existing.get().getPayer().getEmail());
    }

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
            scope,
            jUserSaved,
            jApplication);
    var savedJPayment = jPaymentRepository.save(toSaveJPayment);

    return jPaymentMapper.toDomain(savedJPayment);
  }

  public List<Payment> createPayments(String apiKey, List<PaymentInfo> paymentInfos) {
    var jApplication = jApplicationRepository.findByApiKey(apiKey).get();
    var jPaymentsToSave = new ArrayList<JPayment>();

    var pspPaymentIds = paymentInfos.stream().map(PaymentInfo::pspPaymentId).toList();
    var existingByPspPaymentId =
        jPaymentRepository.findByPspPaymentIdIn(pspPaymentIds).stream()
            .collect(Collectors.toMap(JPayment::getPspPaymentId, Function.identity(), (a, b) -> a));

    for (PaymentInfo paymentInfo : paymentInfos) {
      var existing = existingByPspPaymentId.get(paymentInfo.pspPaymentId());
      if (existing != null) {
        log.warn(
            "Payment with pspType={} and pspPaymentId={} already exists, owned by {}. Skipping.",
            existing.getPspType(),
            existing.getPspPaymentId(),
            existing.getPayer().getEmail());
        continue;
      }

      var jUserSaved = jUserRepository.findByEmail(paymentInfo.payerEmail());
      if (jUserSaved == null) {
        var jUserToSave = new JUser();
        jUserToSave.setEmail(paymentInfo.payerEmail());
        jUserToSave.setId(randomUUID().toString());
        jUserSaved = jUserRepository.save(jUserToSave);
      }

      var toSaveJPayment =
          new JPayment(
              randomUUID().toString(),
              paymentInfo.pspType(),
              null,
              paymentInfo.pspPaymentId(),
              null,
              millisNow(),
              null,
              0,
              null,
              jUserSaved,
              jApplication);
      jPaymentsToSave.add(toSaveJPayment);
    }
    var savedJPayments = jPaymentRepository.saveAll(jPaymentsToSave);
    return savedJPayments.stream().map(jPaymentMapper::toDomain).toList();
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
    return jPaymentRepositoryCustom.findByPaymentInfos(paymentInfos).stream()
        .map(jPaymentMapper::toDomain)
        .toList();
  }

  public List<Payment> findAll() {
    return jPaymentRepository.findAll().stream().map(jPaymentMapper::toDomain).toList();
  }

  public List<Payment> findByApplicationName(String applicationName) {
    return jPaymentRepository.findByApplication_Name(applicationName).stream()
        .map(jPaymentMapper::toDomain)
        .toList();
  }

  public List<Payment> findByApplicationNameAndDateRange(
      String applicationName, String scope, Instant start, Instant end) {
    return jPaymentFilterRepository
        .findByApplicationNameAndCreationInstantBetween(applicationName, scope, start, end)
        .stream()
        .map(jPaymentMapper::toDomain)
        .toList();
  }

  public Page<Payment> findFilteredPage(
      String applicationName, String scope, Instant start, Instant end, Pageable pageable) {
    return jPaymentFilterRepository
        .findFilteredPage(applicationName, scope, start, end, pageable)
        .map(jPaymentMapper::toDomain);
  }

  public long countFiltered(String applicationName, String scope, Instant start, Instant end) {
    return jPaymentFilterRepository.countFiltered(applicationName, scope, start, end);
  }

  public long sumAmountForSucceeded(
      String applicationName, String scope, Instant start, Instant end) {
    return jPaymentFilterRepository.sumAmountForSucceeded(applicationName, scope, start, end);
  }

  public long countPending(String applicationName, String scope, Instant start, Instant end) {
    return jPaymentFilterRepository.countPending(applicationName, scope, start, end);
  }
}
