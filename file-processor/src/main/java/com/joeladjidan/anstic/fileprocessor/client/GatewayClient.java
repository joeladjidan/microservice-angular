package com.joeladjidan.anstic.fileprocessor.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "gateway", url = "${gateway.url}")
public interface GatewayClient {
    @GetMapping("/gateway/health")
    String health();
}

