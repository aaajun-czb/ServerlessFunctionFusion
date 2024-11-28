package com.openfaas.function.proxy;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.fudan.common.util.JsonUtils;

import com.openfaas.function.handler.Handler;

public class Proxy {
    public static void main(String[] args) {
        // 记录启动时间
        long startTime = System.currentTimeMillis();

        // 检查命令行参数
        if (args.length == 0) {
            System.out.println("Usage: java com.openfaas.function.proxy.Proxy <orderId>");
            return;
        }
        // 获取命令行参数
        String orderId = String.join("", args);
        // System.out.println("OrderId: " + orderId);
        // 创建Handler实例
        Handler handler = new Handler();
        // 调用 handle 方法
        Map<String, Object> response = handler.handle(orderId);

        // 输出响应结果
        System.out.println("Response Status: " + response.get("status"));
        System.out.println("Response Message: " + response.get("message"));
        System.out.println("Response Data: " + JsonUtils.object2Json(response.get("data")));

        // 输出执行时间
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution Time: " + executionTime + " ms");
    }
}
