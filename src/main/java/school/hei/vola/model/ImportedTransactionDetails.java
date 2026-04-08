package school.hei.vola.model;

import java.util.List;
import school.hei.vola.model.psp.orange.OrangeTransaction;

public record ImportedTransactionDetails(
    List<OrangeTransaction> failedTransactions, List<OrangeTransaction> successfulTransactions) {}
