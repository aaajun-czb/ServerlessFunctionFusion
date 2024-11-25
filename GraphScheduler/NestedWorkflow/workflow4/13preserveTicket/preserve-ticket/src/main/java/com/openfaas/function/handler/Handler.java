package com.openfaas.function.handler;

import com.openfaas.function.entity.OrderTicketsInfo;
import com.openfaas.function.service.PreserveService;
import com.openfaas.function.service.PreserveServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Handler{

    private PreserveService preserveService = new PreserveServiceImpl();

    public mResponse Handle(Request req) {
        // res.setHeader("Access-Control-Allow-Origin", "*");
        // res.setHeader("Access-Control-Allow-Methods", "POST");
        // res.setHeader("Access-Control-Allow-Headers", "x-requested-with,Authorization,content-type");

        try {
            String reqBody = requestBodyToString(req.body());
            OrderTicketsInfo info = JsonUtils.json2Object(reqBody, OrderTicketsInfo.class);
            mResponse mRes = preserveService.preserve(info);
            return mRes;
        } catch (Exception e) {
            return new mResponse<>(0, e.getMessage(), null);
        }
    }
    public static mResponse UpdateHandle(Request req) {
        try{
            String reqBody = requestBodyToString(req.body());
            
            ObjectMapper mapper = new ObjectMapper();
            // 将字符串解析为JsonNode
            JsonNode rootNode = mapper.readTree(reqBody);
            String function14_URI = rootNode.get("function14_URI").asText();
            // 调用静态方法更新function URI
            PreserveServiceImpl.updateFunctionURI(function14_URI);
            
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
