package com.openfaas.function.service;

import java.util.Map;

public interface RouteService {
    Map<String, Object> getRouteById(String routeId);

}
