package io.github.ayanledeeq1.digitalbanking.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import io.github.ayanledeeq1.digitalbanking.model.Card;

public interface CardRepository extends JpaRepository<Card, Long> {
    boolean existsByCardNumber(String cardNumber);
    @EntityGraph(attributePaths = "customer")
    Optional<Card> findByCustomerEmail(String email);
}
