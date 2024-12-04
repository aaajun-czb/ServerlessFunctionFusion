package com.openfaas.function.handler;

import com.openfaas.function.entity.Route;
import com.openfaas.function.entity.TripId;
import com.openfaas.function.service.TravelService;
import com.openfaas.function.service.TravelServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class Handler {

    private TravelService travelService = new TravelServiceImpl();

    public Map<String, Object> Handle(TripId tripId) {
        Map<String, Object> response = travelService.getRouteByTripId(tripId);
        return response;
    }
}