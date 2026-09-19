package io.github.ayanledeeq1.digitalbanking.dto.customerdto;

public class CustomerRegisterResponseDto {
    private Long id;
    private  String firstName;
    private String lastName;
    private String email;

    public  CustomerRegisterResponseDto(Long id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }


    public  Long getId() {
        return  id;
    }

    public  String getFirstName() {
        return  firstName;
    }

    public  String getLastName() {
        return  lastName;
    }

    public  String gerEmail() {
        return  email;
    }
    
}
