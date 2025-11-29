package com.joeladjidan.anstic.gateway.web;

import com.joeladjidan.anstic.gateway.service.GatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class HealthController {

    private final GatewayService gatewayService;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(gatewayService.health());
    }
}

