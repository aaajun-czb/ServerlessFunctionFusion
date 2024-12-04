package com.openfaas.function.service;

import com.openfaas.function.entity.Station;
import com.openfaas.function.repository.StationRepository;
import com.openfaas.function.repository.StationRepositoryImpl;

import java.util.HashMap;
import java.util.Map;

public class StationServiceImpl implements StationService {

    private StationRepository repository = new StationRepositoryImpl();

    String success = "Success";

    @Override
    public Map<String, Object> queryForId(String stationName) {
        Map<String, Object> response = new HashMap<>();
        try {
            Station station = repository.findByName(stationName);
            if (station != null) {
                response.put("status", 1);
                response.put("message", success);
                response.put("data", station.getId());
            } else {
                response.put("status", 0);
                response.put("message", "Not exists");
                response.put("data", stationName);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        return response;
    }
}