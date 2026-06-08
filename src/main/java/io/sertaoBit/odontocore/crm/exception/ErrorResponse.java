package io.sertaoBit.odontocore.crm.exception;


import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp

) {

    public static ErrorResponse of(HttpStatus httpStatus, String message) {
        return new ErrorResponse(httpStatus.value(), httpStatus.getReasonPhrase(), message, LocalDateTime.now());
    }

}
