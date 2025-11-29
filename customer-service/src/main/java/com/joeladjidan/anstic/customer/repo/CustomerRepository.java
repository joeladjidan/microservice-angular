
package com.joeladjidan.anstic.customer.repo;

import com.joeladjidan.anstic.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}
