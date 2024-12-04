package com.openfaas.function.service;

import com.openfaas.function.entity.Config;
import com.openfaas.function.repository.ConfigRepository;
import com.openfaas.function.repository.ConfigRepositoryImpl;

import java.util.HashMap;
import java.util.Map;

public class ConfigServiceImpl implements ConfigService {
    ConfigRepository repository = new ConfigRepositoryImpl();

    @Override
    public Map<String, Object> query(String name) {
        Config config = repository.findByName(name);
        Map<String, Object> response = new HashMap<>();
        if (config == null) {
            response.put("status", 0);
            response.put("message", "No content");
            response.put("data", null);
        } else {
            response.put("status", 1);
            response.put("message", "Success");
            response.put("data", config);
        }
        return response;
    }
}