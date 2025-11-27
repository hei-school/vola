package school.hei.vola.model;

import school.hei.vola.model.psp.PspType;

public record PaymentInfo(String payerEmail, PspType pspType, String pspPaymentId) {}
