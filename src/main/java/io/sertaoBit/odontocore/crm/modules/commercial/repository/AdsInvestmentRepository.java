package io.sertaoBit.odontocore.crm.modules.commercial.repository;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdsInvestmentRepository extends JpaRepository<AdsInvestment, UUID> {

    List<AdsInvestment> findByChannelAndPeriodStartGreaterThanEqual(AdsChannel channel, LocalDate from);

    BigDecimal sumAmountByChannelAndPeriod(AdsChannel channel, LocalDate from, LocalDate to);
}
