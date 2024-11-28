package com.openfaas.function.proxy;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import edu.fudan.common.util.JsonUtils;

import com.openfaas.function.handler.Handler;

public class Proxy {
    public static void main(String[] args) {
        // 记录启动时间
        long startTime = System.currentTimeMillis();

        // 创建Handler实例
        Handler handler = new Handler();
        String orderId1 = "5ad7750b-a68b-49c0-a8c0-32776b067703";
        String orderId2 = "8177ac5a-61ac-42f4-83f4-bd7b394d0531";
        String orderId3 = "d3c91694-d5b8-424c-9974-e14c89226e49";
        // 随机选择一个orderId
        String[] orderIds = {orderId1, orderId2, orderId3};
        Random random = new Random();
        String orderId = orderIds[random.nextInt(orderIds.length)];

        // 调用Handler的处理方法
        Map<String, Object> response = handler.handle(orderId);

        // 处理响应，例如转换为JSON并输出
        String jsonResponse = JsonUtils.object2Json(response);
        System.out.println("Response: " + jsonResponse);

        // 输出执行时间
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution Time: " + executionTime + " ms");
    }
}