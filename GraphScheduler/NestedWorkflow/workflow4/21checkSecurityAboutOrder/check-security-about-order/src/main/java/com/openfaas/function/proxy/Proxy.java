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
import edu.fudan.common.util.mResponse;

import com.openfaas.function.handler.Handler;

public class Proxy {
    private static final int PORT = 8082;
    private static final int MAX_REQUESTS = 3;
    private static final AtomicInteger requestCount = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {
        // 创建HttpServer实例并绑定到特定地址和端口
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 创建一个上下文，并指定处理器
        server.createContext("/", new InvokeHandler());

        // 设置服务器的线程池
        server.setExecutor(null);

        // 启动服务器
        server.start();

        System.out.println("Server is listening on port " + PORT + " with MAX_REQUESTS = " + MAX_REQUESTS);
    }

    // 定义处理器类
    static class InvokeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int currentCount = requestCount.incrementAndGet();

            // 将exchange转换成okhttp的Request
            Request request = httpExchangeToRequest(exchange);
            
            // 创建Handler实例并处理请求
            Handler handler = new Handler();
            mResponse mRes = handler.Handle(request);

            // 将mReponse转换成exchange进行输出
            sendResponseToExchange(mRes, exchange);
            if (currentCount >= MAX_REQUESTS) {
                exchange.getHttpContext().getServer().stop(0);
                System.out.println("Server stopped after " + currentCount + " requests");
            }
        }
    }

    public static Request httpExchangeToRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String url = exchange.getRequestURI().toString();
        try{
            Request.Builder requestBuilder = new Request.Builder().url("http://locoalhost:"+PORT+url);
            // Add headers from HttpExchange to OkHttp Request
            for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                for (String value : entry.getValue()) {
                    requestBuilder.addHeader(entry.getKey(), value);
                }
            }
            // If the method has a body, add it to the OkHttp Request
            if (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
                byte[] bodyBytes = readAllBytes(exchange.getRequestBody());
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                RequestBody requestBody = RequestBody.create(
                    MediaType.parse(exchange.getRequestHeaders().getFirst("Content-Type")),
                    body
                );
                requestBuilder.method(method, requestBody);
            }
            return requestBuilder.build();
        }catch(Exception e){
            System.out.println(e);
            return null;
        }
    }
    
    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int bytesRead;
        byte[] data = new byte[1024];
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private static void sendResponseToExchange(mResponse response, HttpExchange exchange) throws IOException {
        try{
            // 获取body
            byte[] responseBodyBytes = JsonUtils.object2Json(response).getBytes();

            // 设置状态码
            exchange.sendResponseHeaders(200, responseBodyBytes.length);

            // 设置响应体
            OutputStream os = exchange.getResponseBody();
            os.write(responseBodyBytes);
            os.close();
        }catch(Exception e){
            System.out.println(e);
        }
    }
}