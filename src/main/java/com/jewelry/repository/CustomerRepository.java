package com.jewelry.repository;

import com.jewelry.entity.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Customer-specific repository contract.
 */
public interface CustomerRepository extends BaseRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<Customer> search(String keyword);
}
