package com.openfaas.function.handler;

import com.openfaas.function.service.RouteService;
import com.openfaas.function.service.RouteServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class Handler {
    private RouteService routeService = new RouteServiceImpl();

    public Map<String, Object> Handle(String routeID) {
        Map<String, Object> response = routeService.getRouteById(routeID);
        return response;
    }
}