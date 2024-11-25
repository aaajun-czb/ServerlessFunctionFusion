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
import java.util.concurrent.atomic.AtomicInteger;

import edu.fudan.common.util.JsonUtils;

import com.openfaas.function.entity.OrderInfo;
import com.openfaas.function.handler.Handler;
import java.util.Map;

public class Proxy {
    public static void main(String[] args) {
        // 记录启动时间
        long startTime = System.currentTimeMillis();
        Handler handler = new Handler();
        OrderInfo info = new OrderInfo();
        String loginId = "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f";
        Map<String, Object> response = handler.handle(info, loginId);
        // 输出执行时间
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution Time: " + executionTime + " ms");
    }
}
