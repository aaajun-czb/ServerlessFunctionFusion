package com.openfaas.function.service;

import com.openfaas.function.entity.*;

import java.util.Map;

public interface StationService {

    Map<String, Object> queryForId(String stationName);


}
