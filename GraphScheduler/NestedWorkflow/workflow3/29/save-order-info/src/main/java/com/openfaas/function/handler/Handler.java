package com.openfaas.function.handler;

import com.openfaas.function.entity.Order;
import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import java.util.Map;

public class Handler {

    private OrderService orderService = new OrderServiceImpl();

    public Map<String, Object> handle(Order order) {
        // 调用服务层处理订单
        Map<String, Object> response = orderService.saveChanges(order);

        return response;
    }
}