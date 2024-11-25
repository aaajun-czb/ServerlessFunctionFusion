package com.openfaas.function.handler;
import java.io.IOException;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openfaas.function.entity.OrderInfo;
import com.openfaas.function.service.OrderService;
import com.openfaas.function.service.OrderServiceImpl;
import edu.fudan.common.util.JsonUtils;
import java.util.Map;
import java.util.Collections;

public class Handler {
    private OrderService orderService = new OrderServiceImpl();

    public Map<String, Object> handle(OrderInfo info, String loginId) {
        try {
            return orderService.queryOrdersForRefresh(info, loginId);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap("error", e.getMessage());
        }
    }
}
