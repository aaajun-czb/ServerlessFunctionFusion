package com.openfaas.function.handler;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openfaas.function.service.SecurityService;
import com.openfaas.function.service.SecurityServiceImpl;


import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

public class Handler{
private SecurityService securityService=new SecurityServiceImpl();
    public mResponse Handle(Request req) {
        List<String> segmentsList = req.url().pathSegments();
	    String accountId = segmentsList.get(segmentsList.size()-1);
        mResponse mRes = securityService.check(accountId);

	    return mRes;
    }
    public static mResponse UpdateHandle(Request req) {
        try{
            String reqBody = requestBodyToString(req.body());
            
            ObjectMapper mapper = new ObjectMapper();
            // 将字符串解析为JsonNode
            JsonNode rootNode = mapper.readTree(reqBody);
            String function21_URI = rootNode.get("function21_URI").asText();
            // 调用静态方法更新function URI
            SecurityServiceImpl.updateFunctionURI(function21_URI);
            
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
