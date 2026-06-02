package io.sertaoBit.odontocore.crm.modules.commercial.repository;

import io.sertaoBit.odontocore.crm.core.enums.AdsChannel;
import io.sertaoBit.odontocore.crm.modules.commercial.model.AdsInvestment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdsInvestmentRepository extends JpaRepository<AdsInvestment, UUID> {

    List<AdsInvestment> findByChannelAndPeriodStartGreaterThanEqual(AdsChannel channel, LocalDate from);

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM AdsInvestment a "
            + "WHERE a.channel = :channel " +
            "AND a.periodStart >= :from " +
            "AND a.periodEnd <= :to"
    )
    BigDecimal sumAmountByChannelAndPeriod(
            @Param("channel") AdsChannel channel,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );


    List<AdsInvestment> findByChannelOrderByPeriodStartDesc(AdsChannel channel);

}
