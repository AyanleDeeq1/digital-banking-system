package io.github.ayanledeeq1.digitalbanking.dto.customerdto;

import jakarta.validation.constraints.NotBlank;

public class LogineRequestDto {
    @NotBlank(message = "Customer Email is required")
    private  String email;
    @NotBlank(message = "Passowrd is required")
    private String password;

    public  LogineRequestDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public  String getEmail() {
        return  email;
    }

    public  String getPasssowrd() {
        return  password;
    }
}
