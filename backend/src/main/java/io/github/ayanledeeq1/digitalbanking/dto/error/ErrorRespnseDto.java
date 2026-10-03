package io.github.ayanledeeq1.digitalbanking.dto.error;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;

public class ErrorRespnseDto {
    private  int status;
    private String message;
    private Map<String, List<String>> fieldErrors;

    public ErrorRespnseDto(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public ErrorRespnseDto(int status, String message, Map<String, List<String>> fieldErrors) {
        this(status, message);
        this.fieldErrors = fieldErrors;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, List<String>> getFieldErrors() { return fieldErrors; }

    public  int getStatus() {
        return  status;
    }

    public  String getMassage() {
        return  message;
    }
}
