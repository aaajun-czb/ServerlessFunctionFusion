package com.openfaas.function.service;

import com.openfaas.function.entity.Route;
import com.openfaas.function.repository.RouteRepository;
import com.openfaas.function.repository.RouteRepositoryImpl;

import java.util.HashMap;
import java.util.Map;

public class RouteServiceImpl implements RouteService {
    private RouteRepository routeRepository = new RouteRepositoryImpl();
    String success = "Success";

    @Override
    public Map<String, Object> getRouteById(String routeId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Route route = routeRepository.findById(routeId);
            if (route == null) {
                response.put("status", 0);
                response.put("message", "No content with the routeId");
                response.put("data", routeId);
            } else {
                response.put("status", 1);
                response.put("message", success);
                response.put("data", route);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        return response;
    }
}