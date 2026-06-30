package io.sertaoBit.odontocore.crm.modules.financial.mapper;

import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.InstallmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;

import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.EXPECTED;

@Mapper(componentModel = "spring")
public interface InstallmentMapper {


    @Mapping(target = "overdue", expression = "java(isOverdue(installment))")
    InstallmentResponseDTO toResponseDTO(Installment installment);

    default boolean isOverdue(Installment installment) {
        return installment.getStatus() == EXPECTED
                && installment.getDueDate().isBefore(LocalDate.now());
    }
}
