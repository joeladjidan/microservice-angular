package com.joeladjidan.anstic.customer.web;

import com.joeladjidan.anstic.customer.domain.Customer;
import com.joeladjidan.anstic.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {
  private final CustomerService customerService;

  @GetMapping public List<Customer> all(){ return customerService.findAll(); }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public Customer create(@Valid @RequestBody Customer c){ return customerService.save(c); }

  @GetMapping("/{id}")
  public Customer one(@PathVariable Long id){
    return customerService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
}
