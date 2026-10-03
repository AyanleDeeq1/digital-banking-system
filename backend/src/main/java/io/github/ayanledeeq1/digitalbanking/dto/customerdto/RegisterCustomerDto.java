package io.github.ayanledeeq1.digitalbanking.dto.customerdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class RegisterCustomerDto {
    @NotBlank(message = "Customer firstname is required")
    private String firstName;
    @NotBlank(message = "Customer lastname is required")
    private String lastName;
    @NotBlank(message = "Customer email is required")
    @Email(message = "Enter a valid email address")
    private  String email;
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "At least 8 characters")
    @Pattern(regexp = "(?s).*[A-Z].*", message = "One uppercase letter")
    @Pattern(regexp = "(?s).*[a-z].*", message = "One lowercase letter")
    @Pattern(regexp = "(?s).*[0-9].*", message = "One number")
    @Pattern(regexp = "(?s).*[\\p{P}\\p{S}].*", message = "One special character")
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
