package com.openfaas.function.handler;

import com.openfaas.function.service.StationService;
import com.openfaas.function.service.StationServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class Handler {
    private StationService stationService = new StationServiceImpl();

    public Map<String, Object> Handle(String stationName) {
        Map<String, Object> response = stationService.queryForId(stationName);
        return response;
    }
}