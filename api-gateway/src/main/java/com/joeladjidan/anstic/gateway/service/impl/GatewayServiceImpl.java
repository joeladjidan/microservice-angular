package com.joeladjidan.anstic.gateway.service.impl;

import com.joeladjidan.anstic.gateway.service.GatewayService;
import org.springframework.stereotype.Service;

@Service
public class GatewayServiceImpl implements GatewayService {

    @Override
    public String health() {
        return "OK";
    }
}

