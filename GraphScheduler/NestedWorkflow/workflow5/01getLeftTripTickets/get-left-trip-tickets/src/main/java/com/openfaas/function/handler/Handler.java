package com.openfaas.function.handler;

import com.openfaas.function.repository.TripRepositoryImpl;
import com.openfaas.function.service.TravelServiceImpl;

import com.openfaas.function.entity.*;
import com.openfaas.function.service.TravelService;


import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * function1 queryInfo
 * FINISHED/UNTESTED
 * 根据用户输入返回符合条件的所有列车班次
 * get left trip tickets
 * Http Method : POST
 * <p>
 * 原API地址：ts-travel-service/api/v1/travelservice/trips/left
 * <p>
 * 输入：前端传来的json数据 转成的TripInfo对象
 * 输出：List<TripResponse>
 */

public class Handler{

    private TravelService travelService = new TravelServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");
        String reqBody = requestBodyToString(req.body());
        TripInfo info = JsonUtils.json2Object(reqBody, TripInfo.class);
        mResponse mRes = travelService.query(info);

        // res.setHeader("Access-Control-Allow-Origin","*");
        // res.setHeader("Access-Control-Allow-Methods", "POST");
        // res.setHeader("Access-Control-Allow-Headers", "x-requested-with,content-type");
        return mRes;
    }

    public static mResponse UpdateHandle(Request req) {
        try{
            String reqBody = requestBodyToString(req.body());
            ObjectMapper mapper = new ObjectMapper();
            // 将字符串解析为JsonNode
            JsonNode rootNode = mapper.readTree(reqBody);
            String function8_URI = rootNode.get("function08_URI").asText();
            String function3_URI = rootNode.get("function03_URI").asText();
            String function7_URI = rootNode.get("function07_URI").asText();
            // String function8_URI = "3";
            // 调用静态方法更新function URI
            TravelServiceImpl.updateFunctionURI(function3_URI, function7_URI, function8_URI);
            
            System.out.println("update URI success");

            return new mResponse(1, reqBody, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new mResponse(0, e.getMessage(), null);
        }
    }

    private static String requestBodyToString(RequestBody requestBody) {
        try {
            if (requestBody == null) {
                return null;
            }
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            return buffer.readUtf8();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
