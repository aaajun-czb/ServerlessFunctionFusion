package com.openfaas.function.handler;

import java.util.Map;

import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

public class Handler {
    private OrderService orderService = new OrderServiceImpl();

    public Map<String, Object> handle(String orderId) {
        System.out.println("OrderId: " + orderId);
        // 调用服务层方法获取订单信息
        Map<String, Object> response = orderService.getOrderById(orderId);
        return response;
    }
}
