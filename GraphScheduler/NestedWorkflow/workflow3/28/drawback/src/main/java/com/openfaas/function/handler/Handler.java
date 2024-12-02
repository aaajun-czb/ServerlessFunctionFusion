package com.openfaas.function.handler;

import com.openfaas.function.service.InsidePaymentService;
import com.openfaas.function.service.InsidePaymentServiceImpl;

import java.util.Map;

public class Handler {
    private InsidePaymentService insidePaymentService = new InsidePaymentServiceImpl();

    public Map<String, Object> handle(String userId, double money) {
        // System.out.println("userId: " + userId + " money: " + money);

        // 调用服务方法
        Map<String, Object> response = insidePaymentService.drawBack(userId, money);

        return response;
    }
}