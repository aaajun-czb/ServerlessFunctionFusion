package com.openfaas.function.handler;

import com.openfaas.function.entity.Seat;
import com.openfaas.function.service.SeatService;
import com.openfaas.function.service.SeatServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * function8 getLeftTicketOfInterval
 * <p>
 * get left tickets in an interval
 * Http Method : POST
 * <p>
 * 原API地址： "http://ts-seat-service:18898/api/v1/seatservice/seats/left_tickets"
 * <p>
 * 输入：(Object)Seat
 * 输出：(int)numOfLeftTicket
 */

public class Handler{
    private SeatService seatService = new SeatServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");
        String reqBody = requestBodyToString(req.body());
        Seat seatRequest = JsonUtils.json2Object(reqBody, Seat.class);
        mResponse mRes = seatService.getLeftTicketOfInterval(seatRequest);

        return mRes;
    }

    public static mResponse UpdateHandle(Request req) {
        try{
            String reqBody = requestBodyToString(req.body());
            
            ObjectMapper mapper = new ObjectMapper();
            // 将字符串解析为JsonNode
            JsonNode rootNode = mapper.readTree(reqBody);
            String function09_URI = rootNode.get("function09_URI").asText();
            String function10_URI = rootNode.get("function10_URI").asText();
            String function11_URI = rootNode.get("function11_URI").asText();
            String function12_URI = rootNode.get("function12_URI").asText();
            // 调用静态方法更新function URI
            SeatServiceImpl.updateFunctionURI(function09_URI, function10_URI, function11_URI, function12_URI);
            
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
