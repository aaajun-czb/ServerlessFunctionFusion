package com.openfaas.function.handler;

import com.openfaas.function.service.TravelServiceImpl;

import com.openfaas.function.entity.*;
import com.openfaas.function.service.TravelService;

import java.util.Map;

public class Handler {
    private TravelService travelService = new TravelServiceImpl();
    public Map<String, Object> Handle(TripInfo info) {
        Map<String, Object> response = travelService.query(info);
        return response;
    }
}