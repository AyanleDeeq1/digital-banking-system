package io.github.ayanledeeq1.digitalbanking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.ayanledeeq1.digitalbanking.dto.error.ErrorRespnseDto;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    @ExceptionHandler(AtmException.class)
    public ResponseEntity<ErrorRespnseDto> handleAtm(AtmException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case PIN_REQUIRED, NOT_OWNER -> HttpStatus.FORBIDDEN;
            case INCORRECT_PIN -> HttpStatus.UNAUTHORIZED;
            case ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INACTIVE_ACCOUNT, INSUFFICIENT_FUNDS -> HttpStatus.CONFLICT;
            case INVALID_AMOUNT -> HttpStatus.BAD_REQUEST;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(new ErrorRespnseDto(status.value(), exception.getMessage()));
    }
    @ExceptionHandler(TransferException.class)
    public ResponseEntity<ErrorRespnseDto> handleTransfer(TransferException exception) {
        HttpStatus status = switch (exception.getReason()) {
            case ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case NOT_OWNER -> HttpStatus.FORBIDDEN;
            case INACTIVE_ACCOUNT, INSUFFICIENT_FUNDS -> HttpStatus.CONFLICT;
            case INVALID_TRANSFER -> HttpStatus.BAD_REQUEST;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(new ErrorRespnseDto(status.value(), exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespnseDto> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(new ErrorRespnseDto(400, "Invalid request body"));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespnseDto> handleValidation(
            org.springframework.web.bind.MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        org.springframework.validation.FieldError::getField,
                        java.util.stream.Collectors.mapping(
                                org.springframework.validation.FieldError::getDefaultMessage,
                                java.util.stream.Collectors.toList())));
        return ResponseEntity.badRequest().body(new ErrorRespnseDto(400, "Please check the highlighted fields.", errors));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorRespnseDto> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorRespnseDto(409,
                exception.getMessage(), java.util.Map.of("email", java.util.List.of(exception.getMessage()))));
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorRespnseDto> handleCardNotFound(CardNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorRespnseDto(404, exception.getMessage()));
    }

    @ExceptionHandler(CardIssuanceException.class)
    public ResponseEntity<ErrorRespnseDto> handleCardIssuance(CardIssuanceException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorRespnseDto(503, exception.getMessage()));
    }

    @ExceptionHandler( CustomerNotFoundException.class)
    public  ResponseEntity<ErrorRespnseDto> handleCustomerNotFound(CustomerNotFoundException exception) {
        ErrorRespnseDto error = new ErrorRespnseDto(HttpStatus.NOT_FOUND.value(), exception.getMessage());
        return ResponseEntity
                       .status(HttpStatus.NOT_FOUND)
                       .body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public  ResponseEntity<ErrorRespnseDto> handlleBadCredentialException(BadCredentialsException exception) {
        ErrorRespnseDto error = new ErrorRespnseDto(
            HttpStatus.UNAUTHORIZED.value(),
            "invalid Credentials"     
        );

        return  ResponseEntity
                      .status(HttpStatus.UNAUTHORIZED)
                      .body(error);
    }
}
