package io.github.ayanledeeq1.digitalbanking.dto.customerdto;

import jakarta.validation.constraints.NotBlank;

public class RegisterCustomerDto {
    @NotBlank(message = "Customer firstname is required")
    private String firstName;
    @NotBlank(message = "Customer lastname is required")
    private String lastName;
    @NotBlank(message = "Customer email is required")
    private  String email;
    @NotBlank(message = "Password is required")
    private  String password;

    public  RegisterCustomerDto(String firstname, String lastName, String email, String password ) {
        this.firstName = firstname;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public  String getFirstName() {
        return  firstName;
    }

    public String getLastName() {
        return  lastName;
    }

    public String getEmail() {
        return  email;
    }

    public  String getPassword() {
        return  password;
    }
}
