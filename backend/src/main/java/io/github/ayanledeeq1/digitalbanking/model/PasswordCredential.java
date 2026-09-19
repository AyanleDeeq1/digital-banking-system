package io.github.ayanledeeq1.digitalbanking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class PasswordCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    @Column(nullable = false)
    private String hashedPassword;

    protected  PasswordCredential() {}

    public  PasswordCredential(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public  String getHashedPassword() {
        return  hashedPassword;
    }
}
