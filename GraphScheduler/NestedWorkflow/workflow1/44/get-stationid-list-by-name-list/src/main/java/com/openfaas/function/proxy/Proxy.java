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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.fudan.common.util.JsonUtils;
import edu.fudan.common.util.mResponse;

import com.openfaas.function.handler.Handler;

public class Proxy {
    public static void main(String[] args) {
        // 记录启动时间
        long startTime = System.currentTimeMillis();

        // 创建Handler实例
        Handler handler = new Handler();

        // 模拟调用 handle 方法
        List<String> idList = Arrays.asList("nanjing", "shanghaihongqiao", "shanghai", "beijing", "shanghai", "beijing");
        mResponse response = handler.handle(idList);

        // 输出响应结果
        System.out.println("Response Status: " + response.getStatus());
        System.out.println("Response Message: " + response.getMsg());
        System.out.println("Response Data: " + JsonUtils.object2Json(response.getData()));

        // 输出执行时间
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        System.out.println("Execution Time: " + executionTime + " ms");
    }
}
