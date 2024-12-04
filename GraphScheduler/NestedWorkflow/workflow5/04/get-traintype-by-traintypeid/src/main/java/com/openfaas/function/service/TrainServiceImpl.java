package com.openfaas.function.service;

import com.openfaas.function.entity.TrainType;
import com.openfaas.function.repository.TrainTypeRepository;
import com.openfaas.function.repository.TrainTypeRepositoryImpl;

import java.util.HashMap;
import java.util.Map;

public class TrainServiceImpl implements TrainService {

    private TrainTypeRepository repository = new TrainTypeRepositoryImpl();

    @Override
    public Map<String, Object> retrieve(String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TrainType trainType = repository.findById(id);
            if (trainType == null) {
                response.put("status", 0);
                response.put("message", "here is no TrainType with the trainType id");
                response.put("data", id);
            } else {
                response.put("status", 1);
                response.put("message", "success");
                response.put("data", trainType);
            }
        } catch (Exception e) {
            response.put("status", 0);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        return response;
    }
}