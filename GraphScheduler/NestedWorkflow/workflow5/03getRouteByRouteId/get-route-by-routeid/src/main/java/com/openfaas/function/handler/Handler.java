package com.openfaas.function.handler;

import com.openfaas.function.service.RouteService;
import com.openfaas.function.service.RouteServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * FINISHED
 * function3 getRouteByRouteId
 * 根据用户输入返回符合条件的所有列车班次
 * GET route info
 * Http Method : GET
 * <p>
 * 原API地址：http://ts-route-service:11178/api/v1/routeservice/routes/ + routeId
 * <p>
 * 输入：(String)RouteId
 * 输出：(Object)route
 */

public class Handler{
    private RouteService routeService = new RouteServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");
        List<String> segmentsList = req.url().pathSegments();
	    String routeId = segmentsList.get(segmentsList.size()-1);

        mResponse mRes = routeService.getRouteById(routeId);

        return mRes;
    }
}
