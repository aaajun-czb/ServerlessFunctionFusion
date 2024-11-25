package com.openfaas.function.handler;

import java.util.List;

import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import okhttp3.Request;

public class Handler {
    private OrderService orderService = new OrderServiceImpl();

    public mResponse Handle(Request req) {
        
        // 获取url的最后一个segment，即orderId
        List<String> segmentsList = req.url().pathSegments();
        String orderId = segmentsList.get(segmentsList.size()-1);
        System.out.println("OrderId: " + orderId);

        mResponse mRes = orderService.getOrderById(orderId);

        return mRes;
    }
}
