package com.openfaas.function.handler;

import com.openfaas.function.service.StationService;
import com.openfaas.function.service.StationServiceImpl;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import java.io.IOException;
import java.util.List;

public class Handler{

    private StationService stationService = new StationServiceImpl();

    public mResponse handle(List<String> idList) {
        return stationService.queryByIdBatch(idList);
    }
}
