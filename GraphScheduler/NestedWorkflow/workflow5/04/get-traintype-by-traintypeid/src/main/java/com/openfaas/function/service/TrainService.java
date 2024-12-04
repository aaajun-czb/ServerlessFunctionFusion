package com.openfaas.function.service;

import java.util.Map;
import com.openfaas.function.entity.TrainType;


public interface TrainService {

    Map<String, Object> retrieve(String id);

}
