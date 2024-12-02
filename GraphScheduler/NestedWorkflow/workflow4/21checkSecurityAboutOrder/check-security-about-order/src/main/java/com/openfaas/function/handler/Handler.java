package com.openfaas.function.handler;

import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;

import java.util.Date;

public class Handler {

    private OrderService orderService = new OrderServiceImpl();

    public Object Handle(String accountId, Date checkDate) {
        System.out.println("accountId: " + accountId + " checkDate: " + checkDate);

        // 这里假设orderService.checkSecurityAboutOrder返回一个Object类型的结果
        Object result = orderService.checkSecurityAboutOrder(checkDate, accountId);

        return result;
    }
}