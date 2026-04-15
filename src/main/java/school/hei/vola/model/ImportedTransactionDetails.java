package school.hei.vola.model;

import java.time.Instant;

public record ImportedTransactionDetails(String bucketKey, Instant importDate, String fileName) {}
