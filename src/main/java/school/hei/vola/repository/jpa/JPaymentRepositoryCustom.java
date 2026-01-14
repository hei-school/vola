package school.hei.vola.repository.jpa;

import java.util.List;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.repository.jpa.model.JPayment;

public interface JPaymentRepositoryCustom {
  List<JPayment> findByPaymentInfos(List<PaymentInfo> paymentInfos);
}
