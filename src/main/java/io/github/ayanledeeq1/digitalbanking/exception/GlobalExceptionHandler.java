package io.github.ayanledeeq1.digitalbanking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.ayanledeeq1.digitalbanking.dto.error.ErrorRespnseDto;

@RestControllerAdvice 
public class GlobalExceptionHandler {

    @ExceptionHandler( CustomerNotFoundException.class)
    public  ResponseEntity<ErrorRespnseDto> handleCustomerNotFound(CustomerNotFoundException exception) {
        ErrorRespnseDto error = new ErrorRespnseDto(HttpStatus.NOT_FOUND.value(), exception.getMessage());
        return ResponseEntity
                       .status(HttpStatus.NOT_FOUND)
                       .body(error);
    }
}
