package school.hei.vola.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;

@Repository
public interface JOrangeTransactionRepository extends JpaRepository<JOrangeTransaction, String> {}
