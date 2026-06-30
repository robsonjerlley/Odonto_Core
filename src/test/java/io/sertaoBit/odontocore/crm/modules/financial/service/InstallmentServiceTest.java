package io.sertaoBit.odontocore.crm.modules.financial.service;

import io.sertaoBit.odontocore.crm.config.security.SecurityUtils;
import io.sertaoBit.odontocore.crm.core.enums.PermissionScope;
import io.sertaoBit.odontocore.crm.core.enums.Role;
import io.sertaoBit.odontocore.crm.core.enums.Sector;
import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.request.PayRequestDTO;
import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.CashflowMonthDTO;
import io.sertaoBit.odontocore.crm.modules.financial.domain.model.Installment;
import io.sertaoBit.odontocore.crm.modules.financial.mapper.InstallmentMapper;
import io.sertaoBit.odontocore.crm.modules.financial.repository.CashflowRow;
import io.sertaoBit.odontocore.crm.modules.financial.repository.InstallmentRepository;
import io.sertaoBit.odontocore.crm.modules.financial.service.impl.InstallmentServiceImpl;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.User;
import io.sertaoBit.odontocore.crm.modules.identity.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.EXPECTED;
import static io.sertaoBit.odontocore.crm.core.enums.PaymentStatus.PAID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstallmentService - Testes Unitários")
class InstallmentServiceTest {

    private InstallmentService installmentService;

    @Mock private InstallmentRepository installmentRepository;
    @Mock private InstallmentMapper installmentMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        installmentService = new InstallmentServiceImpl(
                installmentRepository, installmentMapper, securityUtils, permissionService);
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Comercial")
                .username("comercial@test.com")
                .password("hash")
                .sector(Sector.COMMERCIAL)
                .role(Role.USER_COMMERCIAL)
                .active(true)
                .build();
    }

    private Installment buildInstallment(io.sertaoBit.odontocore.crm.core.enums.PaymentStatus status) {
        return Installment.builder()
                .id(UUID.randomUUID())
                .status(status)
                .expectedAmount(new BigDecimal("100.00"))
                .build();
    }

    // ========== PAY ==========

    @Test
    @DisplayName("pay - registra a baixa (paidAmount/paidAt/paidBy) e move EXPECTED → PAID")
    void pay_success() {
        UUID id = UUID.randomUUID();
        User user = buildUser();
        Installment installment = buildInstallment(EXPECTED);
        PayRequestDTO dto = new PayRequestDTO(new BigDecimal("100.00"), LocalDate.now());

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(installmentRepository.findById(id)).thenReturn(Optional.of(installment));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        installmentService.pay(id, dto);

        assertEquals(PAID, installment.getStatus());
        assertEquals(0, dto.paidAmount().compareTo(installment.getPaidAmount()));
        assertEquals(dto.paidAt(), installment.getPaidAt());
        assertEquals(user.getId(), installment.getPaidBy());
        verify(installmentMapper).toResponseDTO(installment);
    }

    @Test
    @DisplayName("pay - lança IllegalStateException quando status != EXPECTED")
    void pay_invalidStatus() {
        UUID id = UUID.randomUUID();
        PayRequestDTO dto = new PayRequestDTO(new BigDecimal("100.00"), LocalDate.now());

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(installmentRepository.findById(id)).thenReturn(Optional.of(buildInstallment(PAID)));

        assertThrows(IllegalStateException.class, () -> installmentService.pay(id, dto));
        verify(installmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("pay - lança ResourceNotFoundException quando a parcela não existe")
    void pay_notFound() {
        UUID id = UUID.randomUUID();
        PayRequestDTO dto = new PayRequestDTO(new BigDecimal("100.00"), LocalDate.now());

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(installmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> installmentService.pay(id, dto));
        verify(installmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("pay - lança AccessDeniedException sem permissão de UPDATE")
    void pay_noPermission() {
        UUID id = UUID.randomUUID();
        PayRequestDTO dto = new PayRequestDTO(new BigDecimal("100.00"), LocalDate.now());

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(installmentRepository.findById(id)).thenReturn(Optional.of(buildInstallment(EXPECTED)));
        doThrow(new AccessDeniedException("Access denied"))
                .when(permissionService).checkOrThrow(any(), any(), any(), any(), any());

        assertThrows(AccessDeniedException.class, () -> installmentService.pay(id, dto));
        verify(installmentRepository, never()).save(any());
    }

    // ========== UNPAY ==========

    @Test
    @DisplayName("unpay - zera paidAmount/paidAt/paidBy e move PAID → EXPECTED")
    void unpay_success() {
        UUID id = UUID.randomUUID();
        Installment installment = buildInstallment(PAID);
        installment.setPaidAmount(new BigDecimal("100.00"));
        installment.setPaidAt(LocalDate.now());
        installment.setPaidBy(UUID.randomUUID());

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(installmentRepository.findById(id)).thenReturn(Optional.of(installment));

        installmentService.unpay(id);

        assertEquals(EXPECTED, installment.getStatus());
        assertNull(installment.getPaidAmount());
        assertNull(installment.getPaidAt());
        assertNull(installment.getPaidBy());
        verify(installmentRepository).save(installment);
    }

    @Test
    @DisplayName("unpay - lança IllegalStateException quando status != PAID")
    void unpay_invalidStatus() {
        UUID id = UUID.randomUUID();

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(installmentRepository.findById(id)).thenReturn(Optional.of(buildInstallment(EXPECTED)));

        assertThrows(IllegalStateException.class, () -> installmentService.unpay(id));
        verify(installmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("unpay - lança ResourceNotFoundException quando a parcela não existe")
    void unpay_notFound() {
        UUID id = UUID.randomUUID();

        when(securityUtils.getCurrentUser()).thenReturn(buildUser());
        when(installmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> installmentService.unpay(id));
        verify(installmentRepository, never()).save(any());
    }

    // ========== READS ==========

    @Test
    @DisplayName("getInstallments - consulta por mês e status dentro do escopo")
    void getInstallments_success() {
        User user = buildUser();
        Pageable pageable = PageRequest.of(0, 10);

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any()))
                .thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(installmentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<?> result = installmentService.getInstallments(YearMonth.now(), EXPECTED, pageable);

        assertNotNull(result);
        verify(installmentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getInstallments - lança AccessDeniedException sem escopo de leitura")
    void getInstallments_noScope() {
        User user = buildUser();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> installmentService.getInstallments(YearMonth.now(), EXPECTED, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("getInstallmentsByCustomerId - lança AccessDeniedException sem escopo de leitura")
    void getInstallmentsByCustomerId_noScope() {
        User user = buildUser();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> installmentService.getInstallmentsByCustomerId(UUID.randomUUID(), PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("cashflow - agrega as linhas do repo em CashflowMonthDTO por mês")
    void cashflow_success() {
        User user = buildUser();

        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any()))
                .thenReturn(Optional.of(PermissionScope.GLOBAL));
        when(installmentRepository.cashflow(any(), any(), eq(PAID), eq(EXPECTED)))
                .thenReturn(List.of(new CashflowRow(2026, 6, new BigDecimal("100"), new BigDecimal("200"))));

        List<CashflowMonthDTO> result =
                installmentService.cashflow(YearMonth.of(2026, 1), YearMonth.of(2026, 12));

        assertEquals(1, result.size());
        assertEquals(YearMonth.of(2026, 6), result.get(0).month());
        assertEquals(0, new BigDecimal("100").compareTo(result.get(0).recebido()));
        assertEquals(0, new BigDecimal("200").compareTo(result.get(0).aReceber()));
    }

    @Test
    @DisplayName("cashflow - lança AccessDeniedException sem escopo de leitura")
    void cashflow_noScope() {
        User user = buildUser();
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(permissionService.getScope(eq(user), any(), any())).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> installmentService.cashflow(YearMonth.of(2026, 1), YearMonth.of(2026, 12)));
    }
}
