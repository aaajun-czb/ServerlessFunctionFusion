package com.openfaas.function.handler;

import com.openfaas.function.entity.Route;
import com.openfaas.function.service.TravelService;
import com.openfaas.function.service.TravelServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * function9 getRouteByTripId
 * <p>
 * get route by tripId
 * Http Method : GET
 * <p>
 * 原API地址： "http://ts-travel-service:12346/api/v1/travelservice/routes/" + trainNumber,
 * <p>
 * 输入：(String)tripId
 * 输出：(Object)Route
 */

public class Handler{

    private TravelService travelService = new TravelServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");
        List<String> segmentsList = req.url().pathSegments();
	    String trainNumber = segmentsList.get(segmentsList.size()-1);
        
        mResponse mRes = travelService.getRouteByTripId(trainNumber);

        return mRes;
    }
    public static mResponse UpdateHandle(Request req) {
        try{
            String reqBody = requestBodyToString(req.body());
            
            ObjectMapper mapper = new ObjectMapper();
            // 将字符串解析为JsonNode
            JsonNode rootNode = mapper.readTree(reqBody);
            String function03_URI = rootNode.get("function03_URI").asText();
            // 调用静态方法更新function URI
            TravelServiceImpl.updateFunctionURI(function03_URI);
            
            System.out.println("update URI success");

            return new mResponse(1, reqBody, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new mResponse(0, e.getMessage(), null);
        }
    }

    private static String requestBodyToString(RequestBody requestBody) throws IOException {
        if (requestBody == null) {
            return null;
        }
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        return buffer.readUtf8();
    }
}
