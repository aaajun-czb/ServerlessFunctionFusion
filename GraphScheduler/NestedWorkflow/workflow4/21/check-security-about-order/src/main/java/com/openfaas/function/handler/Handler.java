package com.openfaas.function.handler;

import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Handler {

    private OrderService orderService = new OrderServiceImpl();

    public Map<String, Object> Handle(String accountId, Date checkDate) {
        // System.out.println("accountId: " + accountId + " checkDate: " + checkDate);

        // 调用orderService.checkSecurityAboutOrder方法
        Map<String, Object> result = orderService.checkSecurityAboutOrder(checkDate, accountId);

        return result;
    }
}