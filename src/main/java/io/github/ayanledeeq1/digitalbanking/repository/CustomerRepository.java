package io.github.ayanledeeq1.digitalbanking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.ayanledeeq1.digitalbanking.model.Customer;

@Repository 
public interface CustomerRepository extends  JpaRepository<Customer, Long>{
    
}
