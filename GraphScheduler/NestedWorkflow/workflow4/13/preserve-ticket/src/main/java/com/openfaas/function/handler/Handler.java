package com.openfaas.function.handler;

import com.openfaas.function.entity.OrderTicketsInfo;
import com.openfaas.function.service.PreserveService;
import com.openfaas.function.service.PreserveServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class Handler {

    private PreserveService preserveService = new PreserveServiceImpl();

    public Map<String, Object> Handle(String accountId) {
        // 创建 OrderTicketsInfo 对象并只填入 accountId
        OrderTicketsInfo info = new OrderTicketsInfo();
        info.setAccountId(accountId);
        Map<String, Object> response = preserveService.preserve(info);
        return response;
    }
}