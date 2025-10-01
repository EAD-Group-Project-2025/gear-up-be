package com.ead.gearup.repository;

import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserEmail(String email);

    Optional<Customer> findByUser(User user);
}