package com.openfaas.function.handler;

import com.openfaas.function.entity.Order;
import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

import java.io.IOException;

public class Handler{

    private OrderService orderService = new OrderServiceImpl();

    public mResponse Handle(Request req) {
        String reqBody = requestBodyToString(req.body());
        Order orderInfo = JsonUtils.json2Object(reqBody, Order.class);
        mResponse mRes = orderService.saveChanges(orderInfo);

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
