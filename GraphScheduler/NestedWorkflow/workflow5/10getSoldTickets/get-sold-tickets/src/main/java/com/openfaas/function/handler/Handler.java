package com.openfaas.function.handler;

import com.openfaas.function.entity.*;
import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * function10 getSoldTickets
 * <p>
 * get ticket list by date and trip id
 * Http Method : POST
 * <p>
 * 原API地址： "http://ts-order-service:12031/api/v1/orderservice/order/tickets"
 * <p>
 * 输入：(object)Seat
 * 输出：(Object)LeftTicketInfo
 */
public class Handler{

    private OrderService orderService = new OrderServiceImpl();

    public mResponse Handle(Request req) {
        System.out.println("start handle");
        String reqBody = requestBodyToString(req.body());
        Seat seatRequest = JsonUtils.json2Object(reqBody, Seat.class);
        mResponse mRes = orderService.getSoldTickets(seatRequest);

        return mRes;
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
