package com.joeladjidan.anstic.order.service;

import com.joeladjidan.anstic.order.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    List<Order> findAll();

    Optional<Order> findById(Long id);

    Order save(Order order);

    void deleteById(Long id);

    List<Order> findByCustomerId(Long customerId);
}

