package io.github.ayanledeeq1.digitalbanking.dto.error;

public class ErrorRespnseDto {
    private  int status;
    private String message;

    public ErrorRespnseDto(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public  int getStatus() {
        return  status;
    }

    public  String getMassage() {
        return  message;
    }
}
