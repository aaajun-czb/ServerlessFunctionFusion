package com.openfaas.function.service;

import com.openfaas.function.entity.*;
import com.openfaas.function.entity.TripId;

import java.util.ArrayList;
import java.util.Map;

public interface TravelService {
    Map<String, Object> getRouteByTripId(TripId tripId);
}
