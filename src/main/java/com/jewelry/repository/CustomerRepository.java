package com.jewelry.repository;

import com.jewelry.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Customer}.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** Keyword search across name and phone. */
    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.firstName) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(c.email)     LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(c.phone)     LIKE LOWER(CONCAT('%', :kw, '%'))
            """)
    List<Customer> search(@Param("kw") String keyword);

    List<Customer> findAllByOrderByLastNameAscFirstNameAsc();
}
