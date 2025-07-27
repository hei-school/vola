package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.repository.UserRepository;

class PaymentServiceIT extends FacadeIT {

  @Autowired PaymentService subject;
  @MockBean EventProducer eventProducerMocked;

  @Autowired UserRepository userRepository;

  @Test
  void createdPayment_canBe_retrieved() {
    var randomLou = userRepository.save(randomLou());
    var randomPspPaymentId = randomUUID().toString();
    var createdPayment = subject.createPayment(randomLou, ORANGE_MONEY, randomPspPaymentId);

    var retrievedPayment = subject.findPaymentById(createdPayment.id());
    assertEquals(createdPayment, retrievedPayment);
    assertNotNull(retrievedPayment.id());
    assertEquals(randomLou, retrievedPayment.payer());
    assertEquals(
        new PspPayment(ORANGE_MONEY, randomPspPaymentId, null, null),
        retrievedPayment.pspPayment());
    assertNotNull(retrievedPayment.creationInstant());
    assertNull(retrievedPayment.lastPspVerificationInstant());
  }

  @Test
  void createdPayment_triggers_verificationEvent() {
    var randomLou = userRepository.save(randomLou());
    var createdPayment = subject.createPayment(randomLou, ORANGE_MONEY, randomUUID().toString());

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMocked, times(1)).accept(captor.capture());
    List<PaymentVerificationRequested> captured = captor.getValue();
    assertEquals(1, captured.size());
    assertEquals(createdPayment, captured.getFirst().getPayment());
  }

  @Test
  void createdPayment_cannotBe_recreated() {
    var randomLou = userRepository.save(randomLou());
    var pspId = randomUUID().toString();
    subject.createPayment(randomLou, ORANGE_MONEY, pspId);

    assertThrows(
        RuntimeException.class, () -> subject.createPayment(randomLou, ORANGE_MONEY, pspId));
  }

  private static User randomLou() {
    return new User("lou+ " + randomUUID() + "@cute.dev");
  }
}
