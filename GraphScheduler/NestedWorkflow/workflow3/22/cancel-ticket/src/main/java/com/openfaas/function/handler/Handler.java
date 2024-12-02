package com.openfaas.function.handler;

import com.openfaas.function.service.CancelService;
import com.openfaas.function.service.CancelServiceImpl;

import java.util.Map;

public class Handler {

    private CancelService cancelService = new CancelServiceImpl();

    public Map<String, Object> handle(String orderId, String loginId) {
        // 调用服务层处理订单取消
        Map<String, Object> response = cancelService.cancelOrder(orderId, loginId);

        return response;
    }
}
