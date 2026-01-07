package school.hei.vola.model;

import lombok.Builder;
import school.hei.vola.model.psp.PspType;

@Builder
public record PaymentInfo(String payerEmail, PspType pspType, String pspPaymentId) {}
