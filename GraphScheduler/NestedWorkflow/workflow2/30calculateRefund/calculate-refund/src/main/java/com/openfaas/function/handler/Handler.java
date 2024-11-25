package com.openfaas.function.handler;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openfaas.function.service.CancelService;
import com.openfaas.function.service.CancelServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

public class Handler{
    private CancelService cancelService = new CancelServiceImpl();

    public mResponse Handle(Request req) {

        // res.setHeader("Access-Control-Allow-Origin", "*");
        // res.setHeader("Access-Control-Allow-Methods", "GET");
        // res.setHeader("Access-Control-Allow-Headers", "x-requested-with,Authorization,content-type");

        try {
            // 获取url的最后一个segment，即orderId
            List<String> segmentsList = req.url().pathSegments();
            String orderId = segmentsList.get(segmentsList.size()-1);
            System.out.println("OrderId: " + orderId);

            mResponse mRes = cancelService.calculateRefund(orderId);
            return mRes;
        } catch (Exception e) {
            e.printStackTrace();
            return new mResponse(0, e.getMessage(), null);
        }

    }
    public static mResponse UpdateHandle(Request req) {
        try{
            String reqBody = requestBodyToString(req.body());
            
            ObjectMapper mapper = new ObjectMapper();
            // 将字符串解析为JsonNode
            JsonNode rootNode = mapper.readTree(reqBody);
            String function23_URI = rootNode.get("function23_URI").asText();
            // 调用静态方法更新function URI
            CancelServiceImpl.updateFunctionURI(function23_URI);
            
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
