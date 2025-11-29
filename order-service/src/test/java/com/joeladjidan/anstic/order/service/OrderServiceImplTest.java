package com.joeladjidan.anstic.order.service;

import com.joeladjidan.anstic.order.domain.Order;
import com.joeladjidan.anstic.order.repository.OrderRepository;
import com.joeladjidan.anstic.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAll_returnsOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order(1L, 1L, new BigDecimal("10.00"))));
        var res = orderService.findAll();
        assertNotNull(res);
        assertEquals(1, res.size());
        verify(orderRepository).findAll();
    }

    @Test
    void findByCustomerId_returnsOnlyCustomerOrders() {
        when(orderRepository.findByCustomerId(1L)).thenReturn(List.of(new Order(1L, 1L, new BigDecimal("10.00"))));
        var res = orderService.findByCustomerId(1L);
        assertEquals(1, res.size());
        verify(orderRepository).findByCustomerId(1L);
    }

    @Test
    void save_delegates() {
        Order o = new Order(null, 2L, new BigDecimal("5.00"));
        when(orderRepository.save(o)).thenReturn(new Order(2L, 2L, new BigDecimal("5.00")));
        var saved = orderService.save(o);
        assertNotNull(saved.getId());
        verify(orderRepository).save(o);
    }
}

