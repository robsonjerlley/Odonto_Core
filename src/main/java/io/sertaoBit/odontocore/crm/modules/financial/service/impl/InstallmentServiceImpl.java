package io.sertaoBit.odontocore.crm.modules.financial.service.impl;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.PaymentStatus;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.request.PayRequestDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.CashflowMonthDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.InstallmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import io.sertaoBit.odontocore.crm.modules.financial.mapper.InstallmentMapper;
import io.sertaoBit.odontocore.crm.modules.financial.repository.InstallmentRepository;
import io.sertaoBit.odontocore.crm.modules.financial.service.InstallmentService;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.Action.READ;
import static io.sertaoBit.odontocore.crm.core.enums.Action.UPDATE;
import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.EXPECTED;
import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.PAID;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.INSTALLMENT;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.COMMERCIAL;
import static io.sertaoBit.odontocore.crm.modules.financial.repository.InstallmentSpecifications.*;


@Service
public class InstallmentServiceImpl implements InstallmentService {

    private final InstallmentRepository installmentRepository;
    private final InstallmentMapper installmentMapper;
    private final SecurityUtils securityUtils;
    private final PermissionService permissionService;

    public InstallmentServiceImpl(
            InstallmentRepository installmentRepository,
            InstallmentMapper installmentMapper,
            SecurityUtils securityUtils,
            PermissionService permissionService
    ) {
        this.installmentRepository = installmentRepository;
        this.installmentMapper = installmentMapper;
        this.securityUtils = securityUtils;
        this.permissionService = permissionService;
    }


    @Override
    @Transactional
    public InstallmentResponseDTO pay(UUID installmentId, PayRequestDTO dto) {
        User user = securityUtils.getCurrentUser();
        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        permissionService.checkOrThrow(
                user,
                INSTALLMENT,
                UPDATE,
                COMMERCIAL,
                user.getId()
        );

        if (installment.getStatus() != EXPECTED) {
            throw new IllegalStateException("Operation Status Not Valid");
        }

        installment.setPaidAmount(dto.paidAmount());
        installment.setPaidAt(dto.paidAt());
        installment.setPaidBy(user.getId());
        installment.setStatus(PAID);

        return installmentMapper.toResponseDTO(installmentRepository.save(installment));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstallmentResponseDTO> getInstallments(YearMonth date, PaymentStatus status, Pageable page) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                INSTALLMENT,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access Denied"));

        var from = date.atDay(1);
        var to = date.atEndOfMonth();

        Specification<Installment> spec = Specification
                .where(byScope(scope, user))
                .and(dueBetween(from, to))
                .and(hasStatus(status));

        return installmentRepository.findAll(spec, page)
                .map(installmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstallmentResponseDTO> getOverdue(Pageable pageable) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                INSTALLMENT,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access Denied"));

        Specification<Installment> spec = Specification
                .where(byScope(scope, user))
                .and(overdue());

        return installmentRepository.findAll(spec, pageable)
                .map(installmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InstallmentResponseDTO> getInstallmentsByCustomerId(UUID customerId, Pageable pageable) {
        User user = securityUtils.getCurrentUser();
        PermissionScope scope = permissionService.getScope(
                user,
                INSTALLMENT,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access Denied"));

        Specification<Installment> spec = Specification
                .where(byScope(scope, user))
                .and(byCustomer(customerId));

        return installmentRepository.findAll(spec, pageable)
                .map(installmentMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashflowMonthDTO> cashflow(YearMonth from, YearMonth to) {
        User user = securityUtils.getCurrentUser();
        permissionService.getScope(
                user,
                INSTALLMENT,
                READ
        ).orElseThrow(() -> new AccessDeniedException("Access Denied"));

        return installmentRepository
                .cashflow(from.atDay(1), to.atEndOfMonth(), PAID, EXPECTED)
                .stream()
                .map(r -> new CashflowMonthDTO(
                        YearMonth.of(r.year(), r.month()), r.recebido(), r.aReceber()))
                .toList();
    }


    @Override
    @Transactional
    public void unpay(UUID installmentId) {
        User user = securityUtils.getCurrentUser();
        Installment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        permissionService.checkOrThrow(
                user,
                INSTALLMENT,
                UPDATE,
                COMMERCIAL,
                user.getId()
        );

        if (installment.getStatus() != PAID) {
            throw new IllegalStateException("Operation Status Not Valid");
        }

        installment.setPaidAmount(null);
        installment.setPaidAt(null);
        installment.setPaidBy(null);
        installment.setStatus(EXPECTED);

        installmentRepository.save(installment);
    }


}
