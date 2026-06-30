package io.sertaoBit.odontocore.crm.modules.financial.repository;

import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, UUID>, JpaSpecificationExecutor<Installment> {

    @Query("""
            select new io.sertaoBit.odontocore.crm.modules.financial.repository.CashflowRow(
                extract(year from i.dueDate),
                extract(month from i.dueDate),
                sum(case when i.status = :paid then i.paidAmount else 0 end),
                sum(case when i.status = :expected then i.expectedAmount else 0 end)
            )
            from Installment i
            where i.dueDate between :from and :to
            group by extract(year from i.dueDate), extract(month from i.dueDate)
            order by extract(year from i.dueDate), extract(month from i.dueDate)
            """)
    List<CashflowRow> cashflow(@Param("from") LocalDate from,
                               @Param("to") LocalDate to,
                               @Param("paid") PaymentStatus paid,
                               @Param("expected") PaymentStatus expected);
}
