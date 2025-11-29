
package com.joeladjidan.anstic.order.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "customerClient", url = "${gateway.base-url}", path = "/api/customers")
public interface CustomerClient {
  @GetMapping("/{id}") CustomerDto getById(@PathVariable("id") Long id);
}
