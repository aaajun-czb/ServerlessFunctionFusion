package com.openfaas.function.service;

import java.util.Map;
import com.openfaas.function.entity.*;

public interface TravelService {
    Map<String, Object> getTrainTypeByTripId(TripId tripId);
}
