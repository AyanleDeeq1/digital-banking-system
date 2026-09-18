package io.github.ayanledeeq1.digitalbanking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.ayanledeeq1.digitalbanking.model.Customer;

@Repository 
public interface CustomerRepository extends  JpaRepository<Customer, Long>{

    @Query("SELECT customer FROM Customer customer WHERE customer.email = :email")
    public Optional<Customer> findByEmail(@Param("email") String email);
    
}
