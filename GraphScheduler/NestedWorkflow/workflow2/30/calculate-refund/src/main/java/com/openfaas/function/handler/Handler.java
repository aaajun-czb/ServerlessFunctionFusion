package com.openfaas.function.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openfaas.function.service.CancelService;
import com.openfaas.function.service.CancelServiceImpl;

import edu.fudan.common.util.JsonUtils;

public class Handler {
    private CancelService cancelService = new CancelServiceImpl();

    public Map<String, Object> handle(String orderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 调用服务层方法计算退款
            Map<String, Object> refundResult = cancelService.calculateRefund(orderId);

            // 将退款结果放入响应中
            response.put("status", 1);
            response.put("data", refundResult);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", 0);
            response.put("message", e.getMessage());
        }
        return response;
    }
}
