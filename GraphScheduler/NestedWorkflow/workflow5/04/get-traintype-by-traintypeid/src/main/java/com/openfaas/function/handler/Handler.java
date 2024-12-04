package com.openfaas.function.handler;

import com.openfaas.function.entity.TrainType;
import com.openfaas.function.service.TrainService;
import com.openfaas.function.service.TrainServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class Handler {
    private TrainService trainService = new TrainServiceImpl();

    public Map<String, Object> Handle(String trainTypeId) {
        Map<String, Object> response = trainService.retrieve(trainTypeId);
        return response;
    }
}