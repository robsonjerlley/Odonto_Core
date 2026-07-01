package io.sertaoBit.odontocore.crm.modules.financial.api.controller;

import io.sertaoBit.odontocore.crm.modules.financial.api.dto.response.InstallmentResponseDTO;
import io.sertaoBit.odontocore.crm.modules.financial.service.InstallmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstallmentController.getOverdue - exclusividade month/overdue")
class InstallmentControllerTest {

    @Mock private InstallmentService installmentService;
    @InjectMocks private InstallmentController controller;

    private final Pageable page = PageRequest.of(0, 20);

    @Test
    @DisplayName("overdue=true e sem month - delega para o service e responde 200")
    void getOverdue_delegates() {
        Page<InstallmentResponseDTO> expected = new PageImpl<>(List.of());
        when(installmentService.getOverdue(page)).thenReturn(expected);

        var response = controller.getOverdue(true, null, page);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(installmentService).getOverdue(page);
    }

    @Test
    @DisplayName("overdue=true combinado com month - 400 e não chama o service")
    void getOverdue_withMonth_badRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getOverdue(true, YearMonth.now(), page));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(installmentService, never()).getOverdue(any());
    }

    @Test
    @DisplayName("overdue=false - 400 e não chama o service")
    void getOverdue_false_badRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getOverdue(false, null, page));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(installmentService, never()).getOverdue(any());
    }
}
