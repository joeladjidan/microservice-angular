package com.joeladjidan.anstic.customer.service;

import com.joeladjidan.anstic.customer.domain.Customer;
import com.joeladjidan.anstic.customer.repo.CustomerRepository;
import com.joeladjidan.anstic.customer.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceImplTest {

    @Mock
    CustomerRepository customerRepository;

    @InjectMocks
    CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAll_returnsAllCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(new Customer(1L, "Alice", "a@x.com")));
        var result = customerService.findAll();
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    void findById_returnsCustomerWhenFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(new Customer(1L, "Bob", "b@x.com")));
        var opt = customerService.findById(1L);
        assertTrue(opt.isPresent());
        assertEquals("Bob", opt.get().getName());
        verify(customerRepository).findById(1L);
    }

    @Test
    void save_delegatesToRepository() {
        Customer c = new Customer(null, "Carol", "c@x.com");
        when(customerRepository.save(c)).thenReturn(new Customer(2L, "Carol", "c@x.com"));
        var saved = customerService.save(c);
        assertNotNull(saved.getId());
        assertEquals("Carol", saved.getName());
        verify(customerRepository).save(c);
    }
}

