package io.sertaoBit.odontocore.crm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DiscountApprovalRequiredException extends RuntimeException {


    public DiscountApprovalRequiredException(String message) {
        super(message);
    }
}
