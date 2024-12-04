package com.openfaas.function.handler;

import com.openfaas.function.service.ConfigService;
import com.openfaas.function.service.ConfigServiceImpl;

import java.util.Map;

public class Handler {
    private ConfigService configService = new ConfigServiceImpl();

    public Map<String, Object> Handle(String configName) {
        return configService.query(configName);
    }
}