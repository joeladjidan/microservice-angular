package com.joeladjidan.anstic.order.web;

import com.joeladjidan.anstic.order.domain.Order;
import com.joeladjidan.anstic.order.service.OrderService;
import com.joeladjidan.anstic.order.client.CustomerClient;
import com.joeladjidan.anstic.order.client.CustomerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;
  private final CustomerClient customerClient;

  @GetMapping public List<Order> all(){ return orderService.findAll(); }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public Map<String,Object> create(@Valid @RequestBody Order o){
    Order saved = orderService.save(o);
    CustomerDto c = customerClient.getById(saved.getCustomerId());
    return Map.of("order", saved, "customer", c);
  }
}
